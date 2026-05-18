package ma.ensate.server.security.ids;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;

public class AdminAnomalyDetector {
    // Stocke le nombre de requêtes "sensibles" par session admin dans la dernière minute
    private static final ConcurrentHashMap<Integer, RequestFloodDetector.RequestCounter> sensitiveAccessCounts = new ConcurrentHashMap<>();
    
    private static final int MAX_SENSITIVE_ACCESS_PER_MINUTE = 5;

    public static void checkAdminAccessTime(int adminId, String ipAddress) {
        LocalTime now = LocalTime.now();
        // Si l'heure est entre 22h et 6h du matin
        if (now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(6, 0))) {
            AlertManager.generateAlert(
                AlertManager.AlertType.ADMIN_ANOMALY, 
                AlertManager.Severity.MEDIUM, 
                ipAddress, 
                adminId, 
                "Connexion Administrateur détectée à une heure inhabituelle (" + now.toString() + ")."
            );
        }
    }

    public static void recordSensitiveDataAccess(int adminId, String ipAddress) {
        long currentMinute = System.currentTimeMillis() / (60 * 1000L);
        
        sensitiveAccessCounts.compute(adminId, (id, counter) -> {
            if (counter == null || counter.secondTimestamp != currentMinute) { // On utilise secondTimestamp pour stocker la minute ici
                return new RequestFloodDetector.RequestCounter(currentMinute, 1);
            }
            
            counter.count++;
            
            if (counter.count == MAX_SENSITIVE_ACCESS_PER_MINUTE) {
                AlertManager.generateAlert(
                    AlertManager.AlertType.ADMIN_ANOMALY, 
                    AlertManager.Severity.HIGH, 
                    ipAddress, 
                    adminId, 
                    "Consultation massive de données sensibles : " + counter.count + " accès en une minute."
                );
            }
            
            return counter;
        });
    }
}
