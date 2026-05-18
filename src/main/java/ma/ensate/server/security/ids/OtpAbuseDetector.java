package ma.ensate.server.security.ids;

import java.util.concurrent.ConcurrentHashMap;

public class OtpAbuseDetector {
    // Compteur d'échecs OTP successifs par utilisateur (userId)
    private static final ConcurrentHashMap<Integer, Integer> failedOtps = new ConcurrentHashMap<>();
    
    private static final int MAX_OTP_FAILURES = 3;

    public static void recordFailedOtp(int userId, String ipAddress) {
        int failures = failedOtps.merge(userId, 1, Integer::sum);
        
        if (failures >= MAX_OTP_FAILURES) {
            AlertManager.generateAlert(
                AlertManager.AlertType.OTP_ABUSE_DETECTED, 
                AlertManager.Severity.HIGH, 
                ipAddress, 
                userId, 
                "Abus OTP : " + failures + " tentatives d'OTP invalides successives pour l'utilisateur ID " + userId
            );
            // Remise à zéro après alerte
            failedOtps.put(userId, 0);
        }
    }

    public static void recordSuccessfulOtp(int userId) {
        // En cas de succès, on réinitialise les compteurs d'échecs successifs
        failedOtps.remove(userId);
    }
}
