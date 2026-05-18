package ma.ensate.server.network;

import ma.ensate.models.*;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.*;
import ma.ensate.server.dao.ClientDAO;
import ma.ensate.server.dao.ProduitDAO;
import ma.ensate.server.dao.UtilisateurDAO;
import ma.ensate.server.services.CommandeService;
import ma.ensate.server.services.PaymentService;
import ma.ensate.server.services.ProductService;
import ma.ensate.server.services.ServicePanier;
import ma.ensate.server.services.UserService;
import ma.ensate.server.security.SYNFloodProtection;
import ma.ensate.server.security.SYNCookieManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ma.ensate.security.SecureHandshake;
import ma.ensate.security.SecureChannel;
import ma.ensate.protocol.dto.HandshakeRequest;
import ma.ensate.protocol.dto.HandshakeResponse;

public class ClientHandler implements Runnable {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket socket;
    private String clientIP;

    private final CommandeService commandeService;
    private final PaymentService  paymentService;
    private final ServicePanier   servicePanier;
    private final ClientDAO       clientDAO;
    private final ProduitDAO      produitDAO;
    private final UtilisateurDAO  utilisateurDAO;
    private final ProductService  productService;
    private final SYNFloodProtection synFloodProtection;
    private final SYNCookieManager synCookieManager;
    private SecureHandshake handshake;
    private SecureChannel secureChannel;

    private static final Set<String> ACTIONS_PUBLIQUES =
            new HashSet<>(Arrays.asList("LOGIN", "REGISTER", "VERIFY_2FA", "GET_SERVER_PUBLIC_KEY", "GENERATE_CHALLENGE_ADMIN", "VERIFY_SIGNATURE_ADMIN", "GET_CAPTCHA_NEW"));

    public ClientHandler(Socket socket) {
        this.socket          = socket;
        this.commandeService = new CommandeService();
        this.paymentService  = new PaymentService();
        this.servicePanier   = new ServicePanier();
        this.clientDAO       = new ClientDAO();
        this.produitDAO      = new ProduitDAO();
        this.utilisateurDAO  = new UtilisateurDAO();
        this.productService  = new ProductService();
        this.synFloodProtection = null;
        this.synCookieManager = null;
    }

    public ClientHandler(Socket socket, SYNFloodProtection synFloodProtection, SYNCookieManager synCookieManager) {
        this.socket          = socket;
        this.commandeService = new CommandeService();
        this.paymentService  = new PaymentService();
        this.servicePanier   = new ServicePanier();
        this.clientDAO       = new ClientDAO();
        this.produitDAO      = new ProduitDAO();
        this.utilisateurDAO  = new UtilisateurDAO();
        this.productService  = new ProductService();
        this.synFloodProtection = synFloodProtection;
        this.synCookieManager = synCookieManager;
    }

    @Override
    public void run() {
        clientIP = socket.getInetAddress().getHostAddress();
        logger.info("Handler démarre pour : " + clientIP);

        ma.ensate.server.security.ids.RequestFloodDetector.recordRequest(clientIP);

        if (synFloodProtection != null) {
            synFloodProtection.confirmConnection(socket.getInetAddress());
        }

        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())
        ) {
            // ════════════════════════════════════════════════════════════
            // PHASE 1: HANDSHAKE SÉCURISÉ RSA/AES
            // ════════════════════════════════════════════════════════════

            System.out.println("[SERVEUR] Attente handshake client...");

            try {
                // 1. Recevoir demande clé publique
                HandshakeRequest clientReq1 = (HandshakeRequest) in.readObject();
                logger.info("Handshake phase 1 reçue");

                // 2. Initialiser handshake et envoyer clé publique
                this.handshake = new SecureHandshake();
                HandshakeResponse response = handshake.sendPublicKey(clientReq1);
                out.writeObject(response);
                out.flush();
                logger.info("Clé publique RSA envoyée au client");

                // 3. Recevoir clé AES chiffrée
                HandshakeRequest clientReq2 = (HandshakeRequest) in.readObject();
                logger.info("Handshake phase 2 reçue");

                // 4. Déchiffrer et établir clé AES partagée
                handshake.receiveEncryptedAESKey(clientReq2);
                HandshakeResponse okResponse = new HandshakeResponse(clientReq2.getNonce(), "HANDSHAKE_COMPLETE");
                out.writeObject(okResponse);
                out.flush();
                logger.info("Handshake complété, AES key établie");

                // 5. Créer canal sécurisé pour requêtes suivantes
                this.secureChannel = new SecureChannel(
                        handshake.getNegotiatedAESKey(),
                        in,
                        out
                );

                System.out.println("[SERVEUR] Canal sécurisé établi");

            } catch (Exception e) {
                logger.error("Erreur handshake: " + e.getMessage());
                return;  // Fermer connexion si handshake échoue
            }

            // ════════════════════════════════════════════════════════════
            // PHASE 2: COMMUNICATION SÉCURISÉE
            // ════════════════════════════════════════════════════════════

            while (true) {
                Request request = secureChannel.readSecureRequest();
                logger.info(" Action reçue : " + request.getAction()
                        + " | Client : " + clientIP);

                if (ma.ensate.server.security.ips.IPSManager.isIPBlocked(clientIP)) {
                    boolean isAdminAction = "GENERATE_CHALLENGE_ADMIN".equals(request.getAction()) || 
                                            "VERIFY_SIGNATURE_ADMIN".equals(request.getAction()) ||
                                            isAdmin(request.getToken());
                    
                    if (!isAdminAction) {
                        logger.warn("🛑 IPS: Action rejetée car l'IP est bloquée " + clientIP);
                        secureChannel.writeSecureResponse(new Response(false, "Votre adresse IP a été bloquée par le système de sécurité."));
                        try { socket.close(); } catch (Exception ignored) {}
                        return; // Fermeture du socket et fin du thread
                    }
                }

                if (!ACTIONS_PUBLIQUES.contains(request.getAction())) {
                    String token = request.getToken();
                    ma.ensate.server.services.SessionManager.SessionResult sResult =
                        ma.ensate.server.services.SessionManager.evaluerEtRegenerer(token, clientIP);

                    if (!sResult.isValid) {
                        logger.warn(" Accès refusé : " + sResult.errorMessage
                                + " | Action : " + request.getAction()
                                + " | Client : " + clientIP);
                        secureChannel.writeSecureResponse(new Response(false, sResult.errorMessage));
                        continue;
                    }

                    if (sResult.latestToken != null) {
                        request.setToken(sResult.latestToken);
                    }

                    Response response = traiterRequete(request);

                    if (sResult.latestToken != null && !sResult.latestToken.equals(token)) {
                        response.setNewToken(sResult.latestToken);
                        try {
                            Utilisateur u = utilisateurDAO.trouverParToken(token);
                            if (u != null) {
                                utilisateurDAO.sauvegarderToken(u.getId(), sResult.latestToken);
                            }
                        } catch (Exception e) {
                            logger.error("Erreur sauvegarde du nouveau token: " + e.getMessage());
                        }
                    }
                    secureChannel.writeSecureResponse(response);
                } else {
                    Response response = traiterRequete(request);
                    secureChannel.writeSecureResponse(response);
                }
            }

        } catch (EOFException e) {
            logger.info(" Client deconnecte : " + clientIP);
        } catch (Exception e) {
            logger.error(" Erreur handler " + clientIP + " : " + e.getMessage());
        }
    }

    private Response traiterRequete(Request request) {
        String action = request.getAction();
        try {
            switch (action) {

                case "LOGIN":
                    return UserService.login(request.getData(), clientIP);

                case "GET_CAPTCHA_NEW": {
                    if (!ma.ensate.server.services.CaptchaService.allowRequest(clientIP)) {
                        return new Response(false, "Limite de requêtes CAPTCHA dépassée. Veuillez patienter 10 secondes.");
                    }
                    ma.ensate.server.services.CaptchaService.CaptchaResult res =
                            ma.ensate.server.services.CaptchaService.generateCaptcha();
                    return new Response(true, "CAPTCHA généré", new Object[]{
                            res.captchaId,
                            res.imageBase64,
                            res.captchaSessionToken
                    });
                }

                case "GENERATE_CHALLENGE_ADMIN":
                    return UserService.genererChallengeAdmin(request.getData());

                case "VERIFY_SIGNATURE_ADMIN":
                    return UserService.loginAdminChallenge(request.getData(), clientIP);

                case "REGISTER":
                    return UserService.register(request.getData());

                case "GET_SERVER_PUBLIC_KEY":
                    return UserService.getServerPublicKey();

                case "LOGOUT":
                    try {
                        Utilisateur u = utilisateurDAO.trouverParToken(request.getToken());
                        if (u != null) {
                            ClientIPRegistry.unregister(u.getId());
                            logger.info("IP supprimée du registry : userId=" + u.getId());
                        }
                    } catch (SQLException e) {
                        logger.warn("Erreur nettoyage registry : " + e.getMessage());
                    }
                    ma.ensate.server.services.SessionManager.endSession(request.getToken());
                    return UserService.logout(request.getData());
                case "GET_PROFIL": {
                    int targetUserId = (int) request.getData();
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetUserId)) {
                        return new Response(false, "Accès non autorisé à ce profil.");
                    }
                    return UserService.getProfil(request.getData());
                }

                case "UPDATE_PROFIL": {
                    Object[] params = (Object[]) request.getData();
                    int targetUserId = (int) params[0];
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetUserId)) {
                        return new Response(false, "Accès non autorisé pour modifier ce profil.");
                    }
                    return UserService.updateProfil(request.getData());
                }

                case "CHANGER_PASSWORD": {
                    Object[] params = (Object[]) request.getData();
                    int targetUserId = (int) params[0];
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetUserId)) {
                        return new Response(false, "Accès non autorisé.");
                    }
                    return UserService.changerMotDePasse(request.getData());
                }

                case "SET_2FA": {
                    Object[] params = (Object[]) request.getData();
                    int targetUserId = (int) params[0];
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetUserId)) {
                        return new Response(false, "Accès non autorisé.");
                    }
                    return UserService.setTwoFa(request.getData());
                }

                case "VERIFY_2FA":
                    return UserService.verifyOtp(request.getData(), clientIP);
                case "GET_ALL_PRODUCTS":
                    return productService.getAllProducts();

                case "GET_PRODUCT_BY_ID":
                    return productService.getProductById(request.getData());

                case "GET_SECURITY_LOGS":
                    return ma.ensate.server.services.SecurityService.getSecurityLogs();

                case "GET_IDS_ALERTS":
                    return ma.ensate.server.services.SecurityService.getSecurityAlerts();

                case "GET_BLOCKED_IPS":
                    return ma.ensate.server.services.SecurityService.getBlockedIPs();

                case "UNBLOCK_IP":
                    return ma.ensate.server.services.SecurityService.unblockIP(request.getData());

                case "GET_BY_CATEGORY":
                    return productService.getProductsByCategory(request.getData());

                case "GET_ALL_CATEGORIES":
                    return productService.getAllCategories();

                case "CREATE_CATEGORY":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action reservee aux administrateurs");
                    }
                    return productService.createCategory(request.getData());

                case "CREATE_PRODUCT":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action reservee aux administrateurs");
                    }
                    return productService.createProduct(request.getData());

                case "UPDATE_PRODUCT":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action reservee aux administrateurs");
                    }
                    return productService.updateProduct(request.getData());

                case "DELETE_PRODUCT":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action reservee aux administrateurs");
                    }
                    return productService.deleteProduct(request.getData());
                case "LISTER_UTILISATEURS":
                    if (isAdmin(request.getToken())) {
                        try {
                            Utilisateur u = utilisateurDAO.trouverParToken(request.getToken());
                            if (u != null) {
                                ma.ensate.server.security.ids.AdminAnomalyDetector.recordSensitiveDataAccess(u.getId(), clientIP);
                            }
                        } catch (Exception e) {}
                    }
                    return UserService.listerUtilisateurs();

                case "SUSPENDRE_COMPTE":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action réservée aux administrateurs");
                    }
                    return UserService.suspendreCompte(request.getData());

                case "REACTIVER_COMPTE":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action réservée aux administrateurs");
                    }
                    return UserService.reactiverCompte(request.getData());
                case "AFFICHER_PANIER": {
                    int targetClientId = Integer.parseInt(request.getData().toString());
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
                        return new Response(false, "Accès non autorisé à ce panier.");
                    }
                    return servicePanier.obtenirPanierResponse(targetClientId);
                }

                case "AJOUTER_AU_PANIER": {
                    String[] parts = request.getData().toString().split(",");
                    int targetClientId = Integer.parseInt(parts[0]);
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
                        return new Response(false, "Accès non autorisé.");
                    }
                    return servicePanier.ajouterProduitResponse(
                            targetClientId,
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                }

                case "SUPPRIMER_DU_PANIER": {
                    String[] parts = request.getData().toString().split(",");
                    int targetClientId = Integer.parseInt(parts[0]);
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
                        return new Response(false, "Accès non autorisé.");
                    }
                    return servicePanier.supprimerProduitResponse(
                            targetClientId,
                            Integer.parseInt(parts[1]));
                }

                case "MODIFIER_QUANTITE_PANIER": {
                    String[] parts = request.getData().toString().split(",");
                    int targetClientId = Integer.parseInt(parts[0]);
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
                        return new Response(false, "Accès non autorisé.");
                    }
                    return servicePanier.modifierQuantiteResponse(
                            targetClientId,
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                }

                case "VIDER_PANIER": {
                    int targetClientId = Integer.parseInt(request.getData().toString());
                    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
                    if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
                        return new Response(false, "Accès non autorisé.");
                    }
                    return servicePanier.viderPanierResponse(targetClientId);
                }

                case "CREER_COMMANDE":
                    return creerCommande(request);

                case "VALIDER_COMMANDE":
                    return validerCommande(request);

                case "CHANGER_STATUT_COMMANDE":
                    return changerStatutCommande(request, clientIP);

                case "GET_ALL_COMMANDES":
                    if (!isAdmin(request.getToken())) {
                        return new Response(false, "Action reservee aux administrateurs");
                    }
                    return getAllCommandes(request);

                case "GET_COMMANDE":
                case "GET_ORDER_BY_ID":
                    return getCommande(request);

                case "GET_HISTORIQUE":
                case "GET_ORDER_HISTORY":
                    return getHistorique(request);

                case "EFFECTUER_PAIEMENT":
                case "PROCESS_PAYMENT":
                    return effectuerPaiement(request);

                case "GET_PAIEMENT":
                    return getPaiement(request);

                case "REGISTER_UDP_PORT": {
                    Object[] udpData = (Object[]) request.getData();
                    int userId = (int) udpData[0];
                    int udpPort = (int) udpData[1];
                    ClientIPRegistry.registerPort(userId, udpPort);
                    logger.info("Port UDP enregistré : userId=" + userId + " port=" + udpPort);
                    return new Response(true, "Port UDP enregistré.");
                }

                default:
                    logger.warn(" Action inconnue : " + action);
                    return new Response(false, "Action inconnue : " + action);
            }
        } catch (Exception e) {
            logger.error(" Erreur traitement action " + action + " : " + e.getMessage());
            return new Response(false, "Erreur : " + e.getMessage());
        }
    }

    private Response creerCommande(Request request) {
        try {
            CreerCommandeRequest req = (CreerCommandeRequest) request.getData();
            if (req == null || req.getLignes() == null || req.getLignes().isEmpty())
                return new Response(false, "La requete doit contenir des lignes de commande");
            if (req.getClientId() <= 0)
                return new Response(false, "ID client invalide");

            // BOLA/IDOR protection
            Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
            if (sessionUserId == null || !sessionUserId.equals(req.getClientId())) {
                return new Response(false, "Accès non autorisé pour créer cette commande.");
            }

            Client client = clientDAO.findById(req.getClientId());
            if (client == null)
                return new Response(false, "Client introuvable : " + req.getClientId());
            List<LigneCommande> lignes = convertirLignesCommande(req.getLignes());
            if (lignes.isEmpty())
                return new Response(false, "Aucune ligne de commande valide");
            Commande commande = commandeService.creerCommande(client, lignes);
            logger.info(" Commande creee : " + commande.getId());
            return new Response(true, "Commande creee avec succes", commande);
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de donnees : " + e.getMessage());
        }
    }

    private Response validerCommande(Request request) {
        try {
            String commandeId = (String) request.getData();
            if (commandeId == null || commandeId.trim().isEmpty())
                return new Response(false, "ID de commande requis");
            Commande commande = commandeService.getCommandeById(commandeId);
            if (commande == null) {
                return new Response(false, "Commande introuvable");
            }

            // BOLA/IDOR protection
            Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
            if (sessionUserId == null || !sessionUserId.equals(commande.getClient().getId())) {
                return new Response(false, "Accès non autorisé.");
            }

            boolean success = commandeService.validerCommande(commandeId);
            return success
                    ? new Response(true,  "Commande validee avec succès")
                    : new Response(false, "Echec de la validation");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response changerStatutCommande(Request request, String clientIP) {
        try {
            // BFLA Protection - Only admins can change order status
            if (!isAdmin(request.getToken())) {
                return new Response(false, "Action réservée aux administrateurs");
            }

            ChangerStatutRequest req = (ChangerStatutRequest) request.getData();
            if (req == null || req.getCommandeId() == null || req.getNouveauStatut() == null)
                return new Response(false, "Requete invalide");
            StatutCommande nouveauStatut;
            try {
                nouveauStatut = StatutCommande.valueOf(req.getNouveauStatut());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Statut invalide : " + req.getNouveauStatut());
            }
            boolean success = commandeService.changerStatutCommande(req.getCommandeId(), nouveauStatut);

            if (success) {
                Commande commande = commandeService.getCommandeById(req.getCommandeId());
                String destinataireIP = null;

                if (commande != null && commande.getClient() != null) {
                    destinataireIP = ClientIPRegistry.getIP(
                            commande.getClient().getId());
                }

                if (destinataireIP != null
                        && nouveauStatut == StatutCommande.VALIDE) {
                    int port = ClientIPRegistry.getPort(
                            commande.getClient().getId()); // ← port unique
                    UDPNotificationServer.notifierCommandeValidee(
                            destinataireIP, port, req.getCommandeId());
                }
                else {
                    logger.warn("Client non connecté, notification ignorée");
                }

                return new Response(true, "Statut mis à jour avec succès");
            }
            else {
                return new Response(false, "Echec de la mise à jour du statut");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response getAllCommandes(Request request) {
        try {
            List<Commande> commandes = commandeService.getAllCommandes();
            return new Response(true, "Commandes récupérées", (Serializable) commandes);
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response getCommande(Request request) {
        try {
            String commandeId = (String) request.getData();
            if (commandeId == null || commandeId.trim().isEmpty())
                return new Response(false, "ID de commande requis");
            Commande commande = commandeService.getCommandeById(commandeId);
            if (commande == null) {
                return new Response(false, "Commande introuvable");
            }

            // BOLA/IDOR protection (Only owner or admin can view order)
            Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
            boolean isAdminUser = isAdmin(request.getToken());
            if (!isAdminUser && (sessionUserId == null || !sessionUserId.equals(commande.getClient().getId()))) {
                return new Response(false, "Accès non autorisé à cette commande.");
            }

            return new Response(true,  "Commande trouvée", commande);
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response getHistorique(Request request) {
        try {
            Integer clientId = (Integer) request.getData();
            if (clientId == null || clientId <= 0)
                return new Response(false, "ID client invalide");

            // BOLA/IDOR protection
            Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
            logger.info("[DEBUG HISTORIQUE] Token=" + request.getToken() + " | clientId=" + clientId + " | sessionUserId=" + sessionUserId);
            if (sessionUserId == null || !sessionUserId.equals(clientId)) {
                return new Response(false, "Accès non autorisé à cet historique.");
            }

            List<Commande> historique = commandeService.getHistorique(clientId);
            return new Response(true, "Historique récupéré", historique);
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response effectuerPaiement(Request request) {
        PaiementRequest req = (PaiementRequest) request.getData();
        if (req == null || req.getCommandeId() == null) {
            return new Response(false, "Requête de paiement invalide");
        }
        try {
            Commande commande = commandeService.getCommandeById(req.getCommandeId());
            if (commande == null) {
                return new Response(false, "Commande introuvable");
            }

            // BOLA/IDOR protection
            Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
            if (sessionUserId == null || !sessionUserId.equals(commande.getClient().getId())) {
                return new Response(false, "Accès non autorisé : vous ne pouvez payer que vos propres commandes.");
            }

            return paymentService.traiterPaiement(req);
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response getPaiement(Request request) {
        try {
            String commandeId = (String) request.getData();
            if (commandeId == null || commandeId.trim().isEmpty())
                return new Response(false, "ID de commande requis");
            Commande commande = commandeService.getCommandeById(commandeId);
            if (commande == null) {
                return new Response(false, "Commande introuvable");
            }

            // BOLA/IDOR protection
            Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
            boolean isAdminUser = isAdmin(request.getToken());
            if (!isAdminUser && (sessionUserId == null || !sessionUserId.equals(commande.getClient().getId()))) {
                return new Response(false, "Accès non autorisé.");
            }

            Paiement paiement = paymentService.getPaiementByCommandeId(commandeId);
            return paiement != null
                    ? new Response(true,  "Paiement trouvé", paiement)
                    : new Response(false, "Paiement introuvable");
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private List<LigneCommande> convertirLignesCommande(List<LigneCommandeDTO> dtos) throws SQLException {
        List<LigneCommande> lignes = new ArrayList<>();
        for (LigneCommandeDTO dto : dtos) {
            if (dto.getProduitId() <= 0 || dto.getQuantite() <= 0) continue;
            Produit produit = produitDAO.findById(dto.getProduitId());
            if (produit == null)
                throw new IllegalArgumentException("Produit introuvable : " + dto.getProduitId());
            lignes.add(new LigneCommande(produit, dto.getQuantite()));
        }
        return lignes;
    }

    private void sendError(String message) {
        try {
        } catch (Exception e) {
            logger.error("Erreur sendError : " + e.getMessage());
        }
    }

    private boolean isAdmin(String token) {
        try {
            Utilisateur utilisateur = utilisateurDAO.trouverParToken(token);
            return utilisateur != null && "ADMINISTRATEUR".equals(utilisateur.getTypeCompte());
        } catch (SQLException e) {
            logger.error("Erreur verification role admin : " + e.getMessage());
            return false;
        }
    }
}
