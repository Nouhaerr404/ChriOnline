package ma.ensate.server.security.ids;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BruteForceDetector {
    // Stocke les timestamps des échecs de connexion par IP
    private static final ConcurrentHashMap<String, List<Long>> failedLogins = new ConcurrentHashMap<>();
    
    private static final int MAX_FAILURES = 3; // 3 échecs maximum
    private static final long TIME_WINDOW_MS = 60 * 1000L; // 1 minute

    public static void recordFailedLogin(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        
        failedLogins.compute(ipAddress, (ip, failures) -> {
            if (failures == null) {
                failures = new ArrayList<>();
            }
            
            // Nettoyer les échecs trop anciens
            failures.removeIf(timestamp -> (currentTime - timestamp) > TIME_WINDOW_MS);
            
            // Ajouter le nouvel échec
            failures.add(currentTime);
            
            // Vérifier s'il y a une attaque
            if (failures.size() >= MAX_FAILURES) {
                AlertManager.generateAlert(
                    AlertManager.AlertType.BRUTE_FORCE_DETECTED, 
                    AlertManager.Severity.HIGH, 
                    ipAddress, 
                    null, 
                    "Attaque par force brute détectée : " + failures.size() + " échecs de connexion en moins d'une minute."
                );
                // On vide la liste après l'alerte pour ne pas spammer d'alertes chaque seconde
                failures.clear(); 
            }
            
            return failures;
        });
    }
}
