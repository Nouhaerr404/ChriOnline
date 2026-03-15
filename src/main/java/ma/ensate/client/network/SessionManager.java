package ma.ensate.client.network;

import ma.ensate.models.Utilisateur;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionManager {

    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    // Singleton
    private static SessionManager instance;

    // L'utilisateur connecté (en mémoire RAM)
    private Utilisateur utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // =============================================
    // SAUVEGARDER L'UTILISATEUR APRÈS LOGIN
    // =============================================
    public void setUtilisateur(Utilisateur u) {
        this.utilisateurConnecte = u;
        if (u != null) {
            logger.info("✅ Session ouverte pour : " + u.getEmail());
        }
    }

    // =============================================
    // RÉCUPÉRER L'UTILISATEUR CONNECTÉ
    // =============================================
    public Utilisateur getUtilisateur() {
        return utilisateurConnecte;
    }

    // =============================================
    // RÉCUPÉRER LE TOKEN
    // utilisé par ClientTCP pour chaque requête
    // =============================================
    public String getToken() {
        if (utilisateurConnecte == null) return null;
        return utilisateurConnecte.getSessionToken();
    }

    // =============================================
    // VÉRIFIER SI QUELQU'UN EST CONNECTÉ
    // =============================================
    public boolean estConnecte() {
        return utilisateurConnecte != null
                && utilisateurConnecte.getSessionToken() != null;
    }

    // =============================================
    // VIDER LA SESSION (LOGOUT)
    // =============================================
    public void clear() {
        if (utilisateurConnecte != null) {
            logger.info("🔌 Session fermée pour : "
                    + utilisateurConnecte.getEmail());
        }
        this.utilisateurConnecte = null;
    }

    // =============================================
    // RACCOURCIS UTILES
    // =============================================
    public String getNomUtilisateur() {
        if (utilisateurConnecte == null) return "";
        return utilisateurConnecte.getNom();
    }

    public boolean estAdmin() {
        if (utilisateurConnecte == null) return false;
        return "ADMINISTRATEUR".equals(utilisateurConnecte.getTypeCompte());
    }
}
