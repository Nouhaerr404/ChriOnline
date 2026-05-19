package ma.ensate.server.services;

import ma.ensate.models.Client;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;
import ma.ensate.security.RSAKeyManager;
import ma.ensate.security.KeyStoreManager;
import ma.ensate.server.dao.UtilisateurDAO;
import ma.ensate.server.network.ClientIPRegistry;
import ma.ensate.util.EmailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;


public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private static final UtilisateurDAO dao = new UtilisateurDAO();
    private static final EmailService emailService = new EmailService();
    
    // Singleton instance of RSA key manager for the server
    private static RSAKeyManager rsaKeyManager;

    static {
        try {
            rsaKeyManager = new RSAKeyManager();
            logger.info("Clé RSA serveur générée avec succès");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Erreur génération clé RSA serveur : " + e.getMessage());
        }
    }

    public static Response register(Object data) {
        try {
            Object[] payload = (Object[]) data;
            Client client = (Client) payload[0];
            String captchaId = (String) payload[1];
            String captchaInput = (String) payload[2];
            String captchaSessionToken = (String) payload[3];

            if (!CaptchaService.verifyCaptcha(captchaId, captchaInput, captchaSessionToken)) {
                return new Response(false, "Code CAPTCHA incorrect ou expiré. Veuillez réessayer.");
            }

            String erreur = validerDonnees(client);
            if (erreur != null) {
                logger.warn("Inscription échouée - données invalides : " + erreur);
                return new Response(false, erreur);
            }


            if (dao.emailExiste(client.getEmail())) {
                logger.warn("Inscription échouée - email déjà utilisé : "
                        + client.getEmail());
                return new Response(false, "Cet email est déjà utilisé !");
            }


            boolean succes = dao.inscrire(client);
            if (succes) {
                logger.info(" Inscription réussie : " + client.getEmail());
                return new Response(true, "Inscription réussie !");
            } else {
                return new Response(false, "Erreur lors de l'inscription.");
            }

        } catch (ClassCastException e) {
            logger.error("Erreur cast données register : " + e.getMessage());
            return new Response(false, "Données invalides.");
        } catch (SQLException e) {
            logger.error("Erreur BD register : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response login(Object data, String clientIP) {
        try {
            Object[] loginPayload = (Object[]) data;
            String email = ((String) loginPayload[0]).trim();
            String password = (String) loginPayload[1];
            String captchaId = (String) loginPayload[2];
            String captchaInput = (String) loginPayload[3];
            String captchaSessionToken = (String) loginPayload[4];

            if (!CaptchaService.verifyCaptcha(captchaId, captchaInput, captchaSessionToken)) {
                return new Response(false, "Code CAPTCHA incorrect ou expiré. Veuillez réessayer.");
            }

            if (dao.estBloque(email)) {
                return new Response(false, dao.getMessageBlocage(email));
            }

            Utilisateur u = dao.trouverParEmailPassword(email, password);

            if (u == null) {
                ma.ensate.server.security.ids.BruteForceDetector.recordFailedLogin(clientIP);
                ma.ensate.server.security.logs.SecurityLogger.logEvent(clientIP, email, ma.ensate.server.security.logs.SecurityLogger.ActionType.LOGIN_FAILED, ma.ensate.server.security.logs.SecurityLogger.Status.FAILURE, "Mauvais email ou mot de passe");
                
                long dureeBlocageMs = dao.enregistrerEchec(email);
                if (dureeBlocageMs > 0) {
                    return new Response(false, dao.getMessageBlocage(email));
                }
                return new Response(false, "Email ou mot de passe incorrect.");
            }

            if ("SUSPENDU".equals(u.getStatut())) {
                return new Response(false, "Compte suspendu. Contactez l'administrateur.");
            }

            Response ipCheck = verifierAccesIPAdmin(u, clientIP, email);
            if (ipCheck != null) {
                ma.ensate.server.security.logs.SecurityLogger.logEvent(clientIP, email, ma.ensate.server.security.logs.SecurityLogger.ActionType.ADMIN_ACCESS, ma.ensate.server.security.logs.SecurityLogger.Status.FAILURE, "Rejet IP : " + clientIP);
                return ipCheck;
            }

            if (dao.estBloque(email)) {
                return new Response(false, dao.getMessageBlocage(email));
            }

            dao.reinitialiserTentatives(email);

            if (u.isTwoFaEnabled()) {
                String otp = OtpStore.generateAndStore(u.getId());
                try {
                    ma.ensate.server.services.EmailService.envoyerCodeOtp(u.getEmail(), otp);
                    logger.info("Code 2FA envoyé à : " + u.getEmail());
                } catch (MessagingException e) {
                    logger.error("Échec envoi email OTP : " + e.getMessage());
                    OtpStore.cancel(u.getId());
                    return new Response(false, "Impossible d'envoyer le code de vérification.");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                return new Response(true, "REQUIRES_2FA",
                        new Object[]{u.getId(), u.getEmail()});
            }

            ma.ensate.server.security.logs.SecurityLogger.logEvent(clientIP, email, ma.ensate.server.security.logs.SecurityLogger.ActionType.LOGIN_SUCCESS, ma.ensate.server.security.logs.SecurityLogger.Status.SUCCESS, "Connexion réussie");
            
            // Si c'est un admin, vérifier l'heure (Anomaly)
            if ("ADMINISTRATEUR".equals(u.getTypeCompte())) {
                ma.ensate.server.security.ids.AdminAnomalyDetector.checkAdminAccessTime(u.getId(), clientIP);
            }

            String token = UUID.randomUUID().toString();
            dao.sauvegarderToken(u.getId(), token);
            u.setSessionToken(token);
            ClientIPRegistry.register(u.getId(), clientIP);
            SessionManager.startSession(token, u.getId(), clientIP);
            logger.info("IP enregistrée pour userId=" + u.getId()
                    + " : " + clientIP);

            logger.info(" Login réussi : " + email);
            return new Response(true, "Connexion réussie !", u);

        } catch (ClassCastException e) {
            logger.error("Erreur cast données login : " + e.getMessage());
            return new Response(false, "Données invalides.");
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Payload login incomplet : " + e.getMessage());
            return new Response(false, "Données de connexion incomplètes.");
        } catch (SQLException e) {
            logger.error("Erreur BD login : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    private static final java.util.Map<String, String> adminChallenges = new java.util.concurrent.ConcurrentHashMap<>();

    public static Response genererChallengeAdmin(Object data) {
        try {
            String email = (String) data;
            Utilisateur u = dao.trouverAdminParEmail(email);
            if (u == null) {
                return new Response(false, "Administrateur introuvable ou accès refusé.");
            }
            String challenge = java.util.UUID.randomUUID().toString();
            adminChallenges.put(email, challenge);
            logger.info("Challenge généré pour l'admin: " + email);
            return new Response(true, "Challenge généré", challenge);
        } catch (Exception e) {
            logger.error("Erreur genererChallengeAdmin : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response loginAdminChallenge(Object data, String clientIP) {
        try {
            Object[] payload = (Object[]) data;
            String email = (String) payload[0];
            String challenge = (String) payload[1];
            String signatureBase64 = (String) payload[2];

            String storedChallenge = adminChallenges.remove(email);
            if (storedChallenge == null || !storedChallenge.equals(challenge)) {
                return new Response(false, "Challenge invalide ou expiré.");
            }

            Utilisateur u = dao.trouverAdminParEmail(email);
            if (u == null) {
                return new Response(false, "Administrateur introuvable.");
            }

            // Load admin public key from TrustStore (PKCS12) instead of database
            String truststorePath = ma.ensate.util.ConfigLoader.get("ADMIN_TRUSTSTORE_PATH", "security/server-admin-truststore.p12");
            String truststorePassword = ma.ensate.util.ConfigLoader.get("ADMIN_TRUSTSTORE_PASSWORD", "serverpass");
            
            java.io.File truststoreFile = new java.io.File(truststorePath);
            if (!truststoreFile.exists()) {
                logger.error("Admin TrustStore introuvable : " + truststorePath);
                return new Response(false, "Erreur serveur : TrustStore admin introuvable.");
            }
            
            KeyStoreManager trustStoreManager = new KeyStoreManager(truststoreFile, truststorePassword);
            java.security.PublicKey publicKey = trustStoreManager.getPublicKey(email);
            if (publicKey == null) {
                return new Response(false, "Certificat introuvable pour cet administrateur dans le TrustStore.");
            }

            boolean isSignatureValid = ma.ensate.security.RSAVerifier.verifyBase64(challenge, signatureBase64, publicKey);

            if (!isSignatureValid) {
                logger.warn("Échec de la vérification de signature RSA pour admin : " + email);
                return new Response(false, "Signature invalide.");
            }

            Response ipCheck = verifierAccesIPAdmin(u, clientIP, email);
            if (ipCheck != null) return ipCheck;

            // Generate session
            String token = java.util.UUID.randomUUID().toString();
            dao.sauvegarderToken(u.getId(), token);
            u.setSessionToken(token);
            ClientIPRegistry.register(u.getId(), clientIP);
            SessionManager.startSession(token, u.getId(), clientIP);

            logger.info("Login Admin RSA réussi : " + email);
            return new Response(true, "Connexion réussie !", u);

        } catch (Exception e) {
            logger.error("Erreur loginAdminChallenge : " + e.getMessage());
            return new Response(false, "Erreur lors de l'authentification.");
        }
    }

    public static Response logout(Object data) {
        try {
            int userId = (int) data;
            dao.supprimerToken(userId);
            logger.info(" Déconnexion userId : " + userId);
            return new Response(true, "Déconnexion réussie.");

        } catch (ClassCastException e) {
            logger.error("Erreur cast données logout : " + e.getMessage());
            return new Response(false, "Données invalides.");
        } catch (SQLException e) {
            logger.error("Erreur BD logout : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static boolean verifierToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                logger.warn(" Tentative d'accès sans token !");
                return false;
            }
            Utilisateur u = dao.trouverParToken(token);
            if (u == null) {
                logger.warn(" Token invalide : " + token);
                return false;
            }
            return true;
        } catch (SQLException e) {
            logger.error("Erreur vérification token : " + e.getMessage());
            return false;
        }
    }

    private static String validerDonnees(Client client) {

        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            return "Le nom est obligatoire.";
        }

        if (client.getEmail() == null || !client.getEmail()
                .matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Email invalide.";
        }

        if (client.getPassword() == null || client.getPassword().length() < 6) {
            return "Le mot de passe doit contenir au moins 6 caractères.";
        }

        if (client.getTel() != null && !client.getTel().isEmpty()) {
            if (!client.getTel().matches("^[0-9+]{8,15}$")) {
                return "Numéro de téléphone invalide.";
            }
        }

        return null;
    }
    public static Response listerUtilisateurs() {
        try {
            List<Utilisateur> liste = dao.findAll();
            logger.info("Liste utilisateurs récupérée : " + liste.size() + " entrées");
            return new Response(true, "Utilisateurs récupérés.", liste);
        } catch (SQLException e) {
            logger.error("Erreur BD listerUtilisateurs : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }
    public static Response suspendreCompte(Object data) {
        try {
            int userId = Integer.parseInt(data.toString());
            boolean ok = dao.suspendreCompte(userId);
            if (ok) {
                logger.info("Compte suspendu par admin : userId=" + userId);
                envoyerNotificationStatutCompte(userId, true);
                return new Response(true, "Compte suspendu avec succès.");
            }
            return new Response(false, "Utilisateur introuvable.");
        } catch (NumberFormatException e) {
            return new Response(false, "ID invalide.");
        } catch (SQLException e) {
            logger.error("Erreur BD suspendreCompte : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }
    public static Response reactiverCompte(Object data) {
        try {
            int userId = Integer.parseInt(data.toString());
            boolean ok = dao.reactiverCompte(userId);
            if (ok) {
                logger.info("Compte réactivé par admin : userId=" + userId);
                envoyerNotificationStatutCompte(userId, false);
                return new Response(true, "Compte réactivé avec succès.");
            }
            return new Response(false, "Utilisateur introuvable.");
        } catch (NumberFormatException e) {
            return new Response(false, "ID invalide.");
        } catch (SQLException e) {
            logger.error("Erreur BD reactiverCompte : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response getProfil(Object data) {
        try {
            int userId = (int) data;
            Utilisateur u = dao.trouverParId(userId);
            if (u == null)
                return new Response(false, "Utilisateur introuvable.");
            logger.info("Profil récupéré pour userId : " + userId);
            return new Response(true, "Profil récupéré.", u);
        } catch (SQLException e) {
            logger.error("Erreur getProfil : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    private static void envoyerNotificationStatutCompte(int userId, boolean suspendu) {
        new Thread(() -> {
            try {
                Utilisateur utilisateur = dao.findById(userId);
                if (utilisateur == null || utilisateur.getEmail() == null || utilisateur.getEmail().isBlank()) {
                    logger.warn("Email introuvable pour notification statut compte: userId={}", userId);
                    return;
                }

                String sujet = suspendu
                        ? "ChriOnline - Suspension de votre compte"
                        : "ChriOnline - Reactivation de votre compte";

                String message = suspendu
                        ? "<p>Bonjour " + utilisateur.getNom() + ",</p>"
                        + "<p>Votre compte ChriOnline a ete suspendu par un administrateur.</p>"
                        + "<p>Si vous pensez qu'il s'agit d'une erreur, contactez le support.</p>"
                        + "<p>Cordialement,<br/>Equipe ChriOnline</p>"
                        : "<p>Bonjour " + utilisateur.getNom() + ",</p>"
                        + "<p>Votre compte ChriOnline a ete reactive. Vous pouvez de nouveau vous connecter.</p>"
                        + "<p>Cordialement,<br/>Equipe ChriOnline</p>";

                emailService.sendHtml(utilisateur.getEmail(), sujet, message);
                logger.info("Email notification statut compte envoye a {}", utilisateur.getEmail());
            } catch (Exception e) {
                // Ne pas impacter la reponse admin si l'email echoue.
                logger.error("Echec envoi email notification statut compte userId={} : {}", userId, e.getMessage());
            }
        }).start();
    }




    public static Response updateProfil(Object data) {
        try {
            Object[] params = (Object[]) data;
            int    userId  = (int)    params[0];
            String nom     = (String) params[1];
            String adresse = (String) params[2];
            String tel     = (String) params[3];

            // Validation
            if (nom == null || nom.trim().isEmpty())
                return new Response(false, "Le nom est obligatoire.");

            if (tel != null && !tel.isEmpty()) {
                if (!tel.matches("^[0-9+]{8,15}$"))
                    return new Response(false, "Numéro de téléphone invalide.");
            }

            boolean success = dao.mettreAJourProfil(userId, nom, adresse, tel);
            if (success) {
                logger.info("Profil mis à jour : userId " + userId);
                return new Response(true, "Profil mis à jour avec succès !");
            }
            return new Response(false, "Échec de la mise à jour.");

        } catch (SQLException e) {
            logger.error("Erreur updateProfil : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }


    public static Response setTwoFa(Object data) {
        try {
            Object[] params = (Object[]) data;
            int     userId  = (int)     params[0];
            boolean enabled = (boolean) params[1];
            boolean ok = dao.setTwoFaEnabled(userId, enabled);
            if (ok) {
                logger.info("2FA mis à jour pour userId=" + userId + " enabled=" + enabled);
                return new Response(true,
                        enabled ? "Authentification à deux facteurs activée."
                                : "Authentification à deux facteurs désactivée.");
            }
            return new Response(false, "Utilisateur introuvable.");
        } catch (SQLException e) {
            logger.error("Erreur setTwoFa : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response verifyOtp(Object data, String clientIP) {
        try {
            Object[] params = (Object[]) data;
            int    userId   = (int)    params[0];
            String code     = (String) params[1];

            if (!OtpStore.validate(userId, code)) {
                ma.ensate.server.security.ids.OtpAbuseDetector.recordFailedOtp(userId, clientIP);
                ma.ensate.server.security.logs.SecurityLogger.logEvent(clientIP, String.valueOf(userId), ma.ensate.server.security.logs.SecurityLogger.ActionType.OTP_VALIDATION_FAILED, ma.ensate.server.security.logs.SecurityLogger.Status.FAILURE, "Code OTP invalide ou expiré");
                logger.warn("Code OTP invalide ou expiré pour userId=" + userId);
                return new Response(false, "Code invalide ou expiré.");
            }

            ma.ensate.server.security.ids.OtpAbuseDetector.recordSuccessfulOtp(userId);
            ma.ensate.server.security.logs.SecurityLogger.logEvent(clientIP, String.valueOf(userId), ma.ensate.server.security.logs.SecurityLogger.ActionType.OTP_VALIDATION_SUCCESS, ma.ensate.server.security.logs.SecurityLogger.Status.SUCCESS, "OTP Validé avec succès");

            Utilisateur u = dao.trouverParId(userId);
            if (u == null) {
                return new Response(false, "Utilisateur introuvable.");
            }

            Response ipCheck = verifierAccesIPAdmin(u, clientIP, String.valueOf(userId));
            if (ipCheck != null) {
                ma.ensate.server.security.logs.SecurityLogger.logEvent(clientIP, String.valueOf(userId), ma.ensate.server.security.logs.SecurityLogger.ActionType.ADMIN_ACCESS, ma.ensate.server.security.logs.SecurityLogger.Status.FAILURE, "Rejet IP pour accès admin");
                return ipCheck;
            }

            String token = UUID.randomUUID().toString();
            dao.sauvegarderToken(u.getId(), token);
            u.setSessionToken(token);
            ClientIPRegistry.register(u.getId(), clientIP);
            SessionManager.startSession(token, u.getId(), clientIP);
            
            // Anomalie admin
            if ("ADMINISTRATEUR".equals(u.getTypeCompte())) {
                ma.ensate.server.security.ids.AdminAnomalyDetector.checkAdminAccessTime(u.getId(), clientIP);
            }

            logger.info("Vérification 2FA réussie pour userId=" + userId);
            return new Response(true, "Connexion réussie !", u);

        } catch (SQLException e) {
            logger.error("Erreur verifyOtp : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response changerMotDePasse(Object data) {
        try {
            Object[] params       = (Object[]) data;
            int    userId         = (int)    params[0];
            String ancienPassword = (String) params[1];
            String nouveauPassword = (String) params[2];

            if (nouveauPassword == null || nouveauPassword.length() < 6)
                return new Response(false,
                        "Le nouveau mot de passe doit contenir au moins 6 caractères.");

            boolean success = dao.changerMotDePasse(
                    userId, ancienPassword, nouveauPassword);
            if (success) {
                logger.info("Mot de passe changé : userId " + userId);
                return new Response(true, "Mot de passe changé avec succès !");
            }
            return new Response(false, "Ancien mot de passe incorrect.");

        } catch (SQLException e) {
            logger.error("Erreur changerMotDePasse : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    /**
     * Centralise la vérification de l'IP pour les administrateurs.
     * @return un Response d'erreur si l'accès est refusé, sinon null.
     */
    private static Response verifierAccesIPAdmin(Utilisateur u, String clientIP, String identifier) {
        if ("ADMINISTRATEUR".equals(u.getTypeCompte()) && !isInternalIP(clientIP)) {
            logger.warn("Accès bloqué - Admin sur IP externe : {} | Identifiant : {}", clientIP, identifier);
            return new Response(false, "Accès administrateur refusé : vous devez être connecté au réseau interne.");
        }
        return null;
    }

    /**
     * Vérifie si l'adresse IP est une adresse interne (réseau privé).
     */
    private static boolean isInternalIP(String ip) {
        if (ip == null || ip.isBlank()) return false;
        return ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")
                || ip.equals("127.0.0.1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1");
    }

    /**
     * Récupère la clé publique RSA du serveur pour le handshake sécurisé
     */
    public static Response getServerPublicKey() {
        try {
            if (rsaKeyManager == null) {
                logger.error("RSA key manager non initialisé");
                return new Response(false, "Erreur serveur: clé RSA non disponible");
            }

            String publicKeyBase64 = rsaKeyManager.getServerPublicKeyBase64();
            logger.info("Clé publique RSA serveur envoyée au client");
            return new Response(true, "Clé publique RSA", publicKeyBase64);

        } catch (Exception e) {
            logger.error("Erreur getServerPublicKey : " + e.getMessage());
            return new Response(false, "Erreur lors de la récupération de la clé publique");
        }
    }
}

