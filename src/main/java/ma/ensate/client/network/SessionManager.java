package ma.ensate.client.network;

import ma.ensate.models.Utilisateur;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionManager {

    private static final Logger logger = LogManager.getLogger(SessionManager.class);


    private static SessionManager instance;

    private int udpPort = 5001;

    public int getUdpPort() { return udpPort; }
    public void setUdpPort(int port) { this.udpPort = port; }

    private Utilisateur utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUtilisateur(Utilisateur u) {
        this.utilisateurConnecte = u;
        if (u != null) {
            logger.info("Session ouverte pour : " + u.getEmail());
        }
    }


    public Utilisateur getUtilisateur() {
        return utilisateurConnecte;
    }

    public int getUserId() {
        return utilisateurConnecte != null ? utilisateurConnecte.getId() : -1;
    }

    public String getToken() {
        if (utilisateurConnecte == null) return null;
        return utilisateurConnecte.getSessionToken();
    }

    public void updateToken(String newToken) {
        if (utilisateurConnecte == null || newToken == null || newToken.isBlank()) {
            return;
        }
        utilisateurConnecte.setSessionToken(newToken);
        logger.info("Token de session mis a jour.");
    }


    public boolean estConnecte() {
        return utilisateurConnecte != null
                && utilisateurConnecte.getSessionToken() != null;
    }

    public void clear() {
        if (utilisateurConnecte != null) {
            logger.info(" Session fermee pour : "
                    + utilisateurConnecte.getEmail());
        }
        this.utilisateurConnecte = null;
    }

    public void logout() {
        clear();
    }

    public String getNomUtilisateur() {
        if (utilisateurConnecte == null) return "";
        return utilisateurConnecte.getNom();
    }

    public boolean estAdmin() {
        if (utilisateurConnecte == null) return false;
        return "ADMINISTRATEUR".equals(utilisateurConnecte.getTypeCompte());
    }
}
