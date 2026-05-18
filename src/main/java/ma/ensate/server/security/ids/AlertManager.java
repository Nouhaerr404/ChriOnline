package ma.ensate.server.security.ids;

import ma.ensate.server.dao.DBConnection;
import ma.ensate.server.security.ips.IPSManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlertManager {
    private static final Logger logger = LogManager.getLogger(AlertManager.class);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public enum AlertType {
        BRUTE_FORCE_DETECTED,
        OTP_ABUSE_DETECTED,
        REQUEST_FLOOD,
        ADMIN_ANOMALY
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public static void generateAlert(AlertType type, Severity severity, String targetIp, Integer targetUserId, String description) {
        // Enregistrement asynchrone de l'alerte
        executor.submit(() -> {
            String sql = "INSERT INTO security_alerts (alert_type, severity, target_ip, target_user_id, description) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, type.name());
                pstmt.setString(2, severity.name());
                pstmt.setString(3, targetIp);
                if (targetUserId != null) {
                    pstmt.setInt(4, targetUserId);
                } else {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                }
                pstmt.setString(5, description);
                
                pstmt.executeUpdate();
                logger.warn("🚨 ALERTE SECURITE [{}] : {}", severity, description);
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de l'alerte : {}", e.getMessage());
            }
        });

        // Déclencher une réaction de l'IPS en fonction de la gravité
        triggerIPS(type, severity, targetIp, targetUserId);
    }

    private static void triggerIPS(AlertType type, Severity severity, String targetIp, Integer targetUserId) {
        if (severity == Severity.CRITICAL || severity == Severity.HIGH) {
            if (targetIp != null && !targetIp.isEmpty()) {
                // Bloquer l'IP pour 30 minutes
                IPSManager.blockIP(targetIp, 30, "Blocage automatique suite à une alerte : " + type.name());
            }
        }
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
