package ma.ensate.server.services;

import ma.ensate.models.Client;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.server.dao.UtilisateurDAO;
import ma.ensate.server.network.ClientIPRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private static final UtilisateurDAO dao = new UtilisateurDAO();

    public static Response register(Object data) {
        try {
            Client client = (Client) data;

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
            String[] credentials = (String[]) data;
            String email    = credentials[0].trim();
            String password = credentials[1];

            if (dao.estBloque(email)) {
                return new Response(false,
                        "Compte bloqué suite à trop de tentatives. " +
                                "Réessayez dans 5 minutes.");
            }

            Utilisateur u = dao.trouverParEmailPassword(email, password);


            if (u == null) {
                dao.enregistrerEchec(email);
                int restantes = 3 - 1; // approximation
                return new Response(false,
                        "Email ou mot de passe incorrect.");
            }

            if ("SUSPENDU".equals(u.getStatut())) {
                return new Response(false, "Compte suspendu. Contactez l'administrateur.");
            }

            if (dao.estBloque(email)) {
                return new Response(false,
                        "Compte bloqué suite à trop de tentatives. " +
                                "Réessayez dans 5 minutes.");
            }


            String token = UUID.randomUUID().toString();
            dao.sauvegarderToken(u.getId(), token);
            dao.reinitialiserTentatives(email);
            u.setSessionToken(token);
            ClientIPRegistry.register(u.getId(), clientIP);
            logger.info("IP enregistrée pour userId=" + u.getId()
                    + " : " + clientIP);

            logger.info(" Login réussi : " + email);
            return new Response(true, "Connexion réussie !", u);

        } catch (ClassCastException e) {
            logger.error("Erreur cast données login : " + e.getMessage());
            return new Response(false, "Données invalides.");
        } catch (SQLException e) {
            logger.error("Erreur BD login : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
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
}
