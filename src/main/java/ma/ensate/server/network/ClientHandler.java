package ma.ensate.server.network;

import ma.ensate.models.*;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.*;
import ma.ensate.server.dao.ClientDAO;
import ma.ensate.server.dao.ProduitDAO;
import ma.ensate.server.services.CommandeService;
import ma.ensate.server.services.PaymentService;
import ma.ensate.server.services.ProductService;
import ma.ensate.server.services.UserService;
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

public class ClientHandler implements Runnable {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket socket;


    private final CommandeService commandeService;
    private final PaymentService  paymentService;
    private final ClientDAO       clientDAO;
    private final ProduitDAO      produitDAO;
    private final ProductService  productService;


    private static final Set<String> ACTIONS_PUBLIQUES =
            new HashSet<>(Arrays.asList("LOGIN", "REGISTER"));

    public ClientHandler(Socket socket) {
        this.socket          = socket;
        this.commandeService = new CommandeService();
        this.paymentService  = new PaymentService();
        this.clientDAO       = new ClientDAO();
        this.produitDAO      = new ProduitDAO();
        this.productService  = new ProductService();
    }

    @Override
    public void run() {
        String clientIP = socket.getInetAddress().getHostAddress();
        logger.info("Handler démarré pour : " + clientIP);

        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                // 1. Recevoir la requête
                Request request = (Request) in.readObject();
                logger.info(" Action reçue : " + request.getAction()
                        + " | Client : " + clientIP);

                // 2. Vérifier le token si action protégée (TP5 - Personne 1)
                if (!ACTIONS_PUBLIQUES.contains(request.getAction())) {
                    String token = request.getToken();
                    if (!UserService.verifierToken(token)) {
                        logger.warn(" Accès refusé — token invalide"
                                + " | Action : " + request.getAction()
                                + " | Client : " + clientIP);
                        out.writeObject(new Response(false,
                                "Non autorisé. Veuillez vous connecter."));
                        out.flush();
                        continue;
                    }
                }

                // 3. Router vers le bon service
                Response response = traiterRequete(request);

                // 4. Envoyer la réponse
                out.writeObject(response);
                out.flush();
            }

        } catch (EOFException e) {
            logger.info(" Client déconnecté : " + clientIP);
        } catch (Exception e) {
            logger.error(" Erreur handler " + clientIP + " : " + e.getMessage());
        }
    }

    private Response traiterRequete(Request request) {
        String action = request.getAction();
        System.out.println("SERVER LOG: Handling action [" + action + "]");
        try {
            switch (action) {

                // ── Personne 1 — Sécurité ──────────────────
                case "LOGIN":
                    return UserService.login(request.getData());

                case "REGISTER":
                    return UserService.register(request.getData());

                case "LOGOUT":
                    return UserService.logout(request.getData());

                // ── Personne 2 — Produits ──────────────────
                case "GET_ALL_PRODUCTS":
                    return productService.getAllProducts();
                case "GET_PRODUCT_BY_ID":
                    return productService.getProductById(request.getData());
                case "GET_BY_CATEGORY":
                    return productService.getProductsByCategory(request.getData());
                case "GET_ALL_CATEGORIES":
                    return productService.getAllCategories();

                // ── Personne 3 — Panier ────────────────────
                case "GET_CART":
                case "ADD_TO_CART":
                case "REMOVE_FROM_CART":
                case "UPDATE_CART":
                case "CLEAR_CART":
                    return new Response(false, "Non implémenté encore.");

                // ── Personne 4 — Commandes ─────────────────
                case "CREER_COMMANDE":
                    return creerCommande(request);

                case "VALIDER_COMMANDE":
                    return validerCommande(request);

                case "CHANGER_STATUT_COMMANDE":
                    return changerStatutCommande(request);

                case "GET_COMMANDE":
                case "GET_ORDER_BY_ID":
                    return getCommande(request);

                case "GET_HISTORIQUE":
                case "GET_ORDER_HISTORY":
                    return getHistorique(request);

                // ── Personne 4 — Paiement ──────────────────
                case "EFFECTUER_PAIEMENT":
                case "PROCESS_PAYMENT":
                    return effectuerPaiement(request);

                case "GET_PAIEMENT":
                    return getPaiement(request);

                default:
                    logger.warn(" Action inconnue : " + action);
                    return new Response(false, "Action inconnue : " + action);
            }
        } catch (Exception e) {
            logger.error(" Erreur traitement action " + action
                    + " : " + e.getMessage());
            return new Response(false, "Erreur : " + e.getMessage());
        }
    }


    private Response creerCommande(Request request) {
        try {
            CreerCommandeRequest req = (CreerCommandeRequest) request.getData();

            if (req == null || req.getLignes() == null || req.getLignes().isEmpty())
                return new Response(false, "La requête doit contenir des lignes de commande");

            if (req.getClientId() <= 0)
                return new Response(false, "ID client invalide");

            Client client = clientDAO.findById(req.getClientId());
            if (client == null)
                return new Response(false, "Client introuvable : " + req.getClientId());

            List<LigneCommande> lignes = convertirLignesCommande(req.getLignes());
            if (lignes.isEmpty())
                return new Response(false, "Aucune ligne de commande valide");

            Commande commande = commandeService.creerCommande(client, lignes);
            logger.info(" Commande créée : " + commande.getId());
            return new Response(true, "Commande créée avec succès", commande);

        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response validerCommande(Request request) {
        try {
            String commandeId = (String) request.getData();
            if (commandeId == null || commandeId.trim().isEmpty())
                return new Response(false, "ID de commande requis");

            boolean success = commandeService.validerCommande(commandeId);
            return success
                    ? new Response(true,  "Commande validée avec succès")
                    : new Response(false, "Échec de la validation");

        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response changerStatutCommande(Request request) {
        try {
            ChangerStatutRequest req = (ChangerStatutRequest) request.getData();
            if (req == null || req.getCommandeId() == null || req.getNouveauStatut() == null)
                return new Response(false, "Requête invalide");

            StatutCommande nouveauStatut;
            try {
                nouveauStatut = StatutCommande.valueOf(req.getNouveauStatut());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Statut invalide : " + req.getNouveauStatut());
            }

            boolean success = commandeService.changerStatutCommande(
                    req.getCommandeId(), nouveauStatut);
            return success
                    ? new Response(true,  "Statut mis à jour avec succès")
                    : new Response(false, "Échec de la mise à jour du statut");

        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
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
            return commande != null
                    ? new Response(true,  "Commande trouvée", commande)
                    : new Response(false, "Commande introuvable");

        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response getHistorique(Request request) {
        try {
            Integer clientId = (Integer) request.getData();
            if (clientId == null || clientId <= 0)
                return new Response(false, "ID client invalide");

            List<Commande> historique = commandeService.getHistorique(clientId);
            return new Response(true, "Historique récupéré", historique);

        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }


    private Response effectuerPaiement(Request request) {
        try {
            PaiementRequest req = (PaiementRequest) request.getData();
            if (req == null || req.getCommandeId() == null || req.getMethodePaiement() == null)
                return new Response(false, "Requête de paiement invalide");

            MethodePaiement methode;
            try {
                methode = MethodePaiement.valueOf(req.getMethodePaiement());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Méthode de paiement invalide : "
                        + req.getMethodePaiement());
            }

            if (methode == MethodePaiement.CARTE_BANCAIRE) {
                if (req.getCardLast4() == null || req.getCardLast4().length() != 4)
                    return new Response(false,
                            "Les 4 derniers chiffres de la carte sont requis");
            }

            Commande commande = commandeService.getCommandeById(req.getCommandeId());
            if (commande == null)
                return new Response(false, "Commande introuvable");

            boolean success = paymentService.effectuerPaiement(
                    commande, methode, req.getCardLast4());

            if (success) {
                Paiement paiement = paymentService.getPaiementByCommandeId(
                        req.getCommandeId());
                logger.info("Paiement effectué : " + req.getCommandeId());
                return new Response(true, "Paiement effectué avec succès", paiement);
            } else {
                return new Response(false, "Échec du paiement");
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    private Response getPaiement(Request request) {
        try {
            String commandeId = (String) request.getData();
            if (commandeId == null || commandeId.trim().isEmpty())
                return new Response(false, "ID de commande requis");

            Paiement paiement = paymentService.getPaiementByCommandeId(commandeId);
            return paiement != null
                    ? new Response(true,  "Paiement trouvé", paiement)
                    : new Response(false, "Paiement introuvable");

        } catch (SQLException e) {
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }


    private List<LigneCommande> convertirLignesCommande(
            List<LigneCommandeDTO> dtos) throws SQLException {
        List<LigneCommande> lignes = new ArrayList<>();
        for (LigneCommandeDTO dto : dtos) {
            if (dto.getProduitId() <= 0 || dto.getQuantite() <= 0) continue;
            Produit produit = produitDAO.findById(dto.getProduitId());
            if (produit == null)
                throw new IllegalArgumentException(
                        "Produit introuvable : " + dto.getProduitId());
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
}