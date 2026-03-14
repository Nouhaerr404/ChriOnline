package ma.ensate.server.services;

import ma.ensate.models.Client;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.server.dao.UtilisateurDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.UUID;

public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private static final UtilisateurDAO dao = new UtilisateurDAO();

    // =============================================
    // REGISTER
    // =============================================
    public static Response register(Object data) {
        try {
            Client client = (Client) data;

            // 1. Validation des données
            String erreur = validerDonnees(client);
            if (erreur != null) {
                logger.warn("Inscription échouée - données invalides : " + erreur);
                return new Response(false, erreur);
            }

            // 2. Vérifier si email déjà utilisé
            if (dao.emailExiste(client.getEmail())) {
                logger.warn("Inscription échouée - email déjà utilisé : "
                        + client.getEmail());
                return new Response(false, "Cet email est déjà utilisé !");
            }

            // 3. Inscrire
            boolean succes = dao.inscrire(client);
            if (succes) {
                logger.info("✅ Inscription réussie : " + client.getEmail());
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

    // =============================================
    // LOGIN
    // =============================================
    public static Response login(Object data) {
        try {
            String[] credentials = (String[]) data;
            String email    = credentials[0].trim();
            String password = credentials[1];

            // 1. Vérifier si compte bloqué (TP1)
            if (dao.estBloque(email)) {
                return new Response(false,
                        "Compte bloqué suite à trop de tentatives. " +
                                "Réessayez dans 5 minutes.");
            }

            // 2. Chercher dans la BD
            Utilisateur u = dao.trouverParEmailPassword(email, password);

            // 3. Échec login
            if (u == null) {
                dao.enregistrerEchec(email);
                int restantes = 3 - 1; // approximation
                return new Response(false,
                        "Email ou mot de passe incorrect.");
            }

            // 4. Succès → générer token UUID (TP5)
            String token = UUID.randomUUID().toString();
            dao.sauvegarderToken(u.getId(), token);
            dao.reinitialiserTentatives(email);
            u.setSessionToken(token);

            logger.info("✅ Login réussi : " + email);
            return new Response(true, "Connexion réussie !", u);

        } catch (ClassCastException e) {
            logger.error("Erreur cast données login : " + e.getMessage());
            return new Response(false, "Données invalides.");
        } catch (SQLException e) {
            logger.error("Erreur BD login : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    // =============================================
    // LOGOUT
    // =============================================
    public static Response logout(Object data) {
        try {
            int userId = (int) data;
            dao.supprimerToken(userId);
            logger.info("✅ Déconnexion userId : " + userId);
            return new Response(true, "Déconnexion réussie.");

        } catch (ClassCastException e) {
            logger.error("Erreur cast données logout : " + e.getMessage());
            return new Response(false, "Données invalides.");
        } catch (SQLException e) {
            logger.error("Erreur BD logout : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    // =============================================
    // VÉRIFIER TOKEN (TP5)
    // utilisé par ClientHandler pour protéger
    // TOUTES les autres requêtes
    // =============================================
    public static boolean verifierToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                logger.warn("⛔ Tentative d'accès sans token !");
                return false;
            }
            Utilisateur u = dao.trouverParToken(token);
            if (u == null) {
                logger.warn("⛔ Token invalide : " + token);
                return false;
            }
            return true;
        } catch (SQLException e) {
            logger.error("Erreur vérification token : " + e.getMessage());
            return false;
        }
    }

    // =============================================
    // VALIDATION DES DONNÉES (TP7)
    // =============================================
    private static String validerDonnees(Client client) {

        // Nom
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            return "Le nom est obligatoire.";
        }

        // Email
        if (client.getEmail() == null || !client.getEmail()
                .matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Email invalide.";
        }

        // Password — min 6 caractères
        if (client.getPassword() == null || client.getPassword().length() < 6) {
            return "Le mot de passe doit contenir au moins 6 caractères.";
        }

        // Téléphone — optionnel mais si présent doit être valide
        if (client.getTel() != null && !client.getTel().isEmpty()) {
            if (!client.getTel().matches("^[0-9+]{8,15}$")) {
                return "Numéro de téléphone invalide.";
            }
        }

        return null; // tout est valide
    }
}
