package ma.ensate.server.services;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionManager {
    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    // Durée de vie d'une session par inactivité (ex: 30 minutes)
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; 
    
    // Fréquence de régénération du token (ex: 5 minutes)
    private static final long REGENERATION_INTERVAL_MS = 5 * 60 * 1000;

    static class SessionDetails {
        int userId;
        long lastAccessTime;
        long tokenCreationTime;

        public SessionDetails(int userId) {
            this.userId = userId;
            this.lastAccessTime = System.currentTimeMillis();
            this.tokenCreationTime = System.currentTimeMillis();
        }
    }

    private static final ConcurrentHashMap<String, SessionDetails> activeSessions = new ConcurrentHashMap<>();

    public static void startSession(String token, int userId) {
        activeSessions.put(token, new SessionDetails(userId));
    }

    public static SessionResult evaluerEtRegenerer(String currentToken) {
        if (currentToken == null || currentToken.isEmpty()) {
            return new SessionResult(false, null, "Non autorisé. Veuillez vous connecter.");
        }
        
        SessionDetails details = activeSessions.get(currentToken);
        long currentTime = System.currentTimeMillis();

        if (details == null) {
            return new SessionResult(false, null, "Session introuvable ou non-connectée. Veuillez vous reconnecter.");
        }

        // 1. Expiration (inactivité)
        if (currentTime - details.lastAccessTime > SESSION_TIMEOUT_MS) {
            activeSessions.remove(currentToken);
            logger.warn("Session expirée (inactivité) pour userId: " + details.userId);
            return new SessionResult(false, null, "Session expirée. Veuillez vous reconnecter.");
        }

        details.lastAccessTime = currentTime;

        // 2. Régénération Anti-Hijacking
        if (currentTime - details.tokenCreationTime > REGENERATION_INTERVAL_MS) {
            String newToken = UUID.randomUUID().toString();
            details.tokenCreationTime = currentTime;
            
            activeSessions.put(newToken, details);
            activeSessions.remove(currentToken);
            
            logger.info("Session régénérée (Anti-Hijacking). UserId: " + details.userId);
            return new SessionResult(true, newToken, null);
        }

        return new SessionResult(true, currentToken, null);
    }
    
    public static void endSession(String token) {
        if(token != null) activeSessions.remove(token);
    }

    public static class SessionResult {
        public boolean isValid;
        public String latestToken; 
        public String errorMessage;

        public SessionResult(boolean isValid, String latestToken, String errorMessage) {
            this.isValid = isValid;
            this.latestToken = latestToken;
            this.errorMessage = errorMessage;
        }
    }
}
