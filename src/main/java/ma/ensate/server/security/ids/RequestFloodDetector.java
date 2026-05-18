package ma.ensate.server.security.ids;

import java.util.concurrent.ConcurrentHashMap;

public class RequestFloodDetector {
    // Compteur de requêtes par IP dans la seconde actuelle
    private static final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    // Règle: Maximum 50 requêtes par seconde par IP
    private static final int MAX_REQUESTS_PER_SECOND = 50;

    static class RequestCounter {
        long secondTimestamp;
        int count;

        RequestCounter(long secondTimestamp, int count) {
            this.secondTimestamp = secondTimestamp;
            this.count = count;
        }
    }

    public static void recordRequest(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) return;

        long currentSecond = System.currentTimeMillis() / 1000L;
        
        requestCounts.compute(ipAddress, (ip, counter) -> {
            if (counter == null || counter.secondTimestamp != currentSecond) {
                // Nouvelle seconde, on réinitialise le compteur
                return new RequestCounter(currentSecond, 1);
            }
            
            // Même seconde, on incrémente
            counter.count++;
            
            if (counter.count == MAX_REQUESTS_PER_SECOND) { // On déclenche une seule fois par seconde
                AlertManager.generateAlert(
                    AlertManager.AlertType.REQUEST_FLOOD, 
                    AlertManager.Severity.MEDIUM, 
                    ipAddress, 
                    null, 
                    "Déluge de requêtes (Flood) : Plus de " + MAX_REQUESTS_PER_SECOND + " requêtes par seconde détectées."
                );
            }
            
            return counter;
        });
    }
}
