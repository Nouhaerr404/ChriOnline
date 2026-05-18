package ma.ensate.server.services;

import ma.ensate.models.BlockedIP;
import ma.ensate.models.SecurityAlert;
import ma.ensate.models.SecurityLog;
import ma.ensate.protocol.Response;
import ma.ensate.server.dao.DBConnection;
import ma.ensate.server.security.ips.IPSManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SecurityService {
    private static final Logger logger = LogManager.getLogger(SecurityService.class);

    public static Response getSecurityLogs() {
        List<SecurityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 500";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                logs.add(new SecurityLog(
                        rs.getInt("id"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("ip_address"),
                        rs.getString("user_identifier"),
                        rs.getString("action_type"),
                        rs.getString("status"),
                        rs.getString("details")
                ));
            }
            return new Response(true, "Logs récupérés", logs);
        } catch (Exception e) {
            logger.error("Erreur getSecurityLogs : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response getSecurityAlerts() {
        List<SecurityAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM security_alerts ORDER BY timestamp DESC LIMIT 200";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                alerts.add(new SecurityAlert(
                        rs.getInt("id"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("alert_type"),
                        rs.getString("severity"),
                        rs.getString("target_ip"),
                        rs.getObject("target_user_id") != null ? rs.getInt("target_user_id") : null,
                        rs.getString("description")
                ));
            }
            return new Response(true, "Alertes récupérées", alerts);
        } catch (Exception e) {
            logger.error("Erreur getSecurityAlerts : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response getBlockedIPs() {
        List<BlockedIP> blockedIPs = new ArrayList<>();
        String sql = "SELECT * FROM blocked_ips ORDER BY blocked_until DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                blockedIPs.add(new BlockedIP(
                        rs.getString("ip_address"),
                        rs.getTimestamp("blocked_until"),
                        rs.getString("reason")
                ));
            }
            return new Response(true, "IPs bloquées récupérées", blockedIPs);
        } catch (Exception e) {
            logger.error("Erreur getBlockedIPs : " + e.getMessage());
            return new Response(false, "Erreur serveur.");
        }
    }

    public static Response unblockIP(Object data) {
        try {
            String ipAddress = (String) data;
            IPSManager.unblockIP(ipAddress);
            return new Response(true, "L'IP " + ipAddress + " a été débloquée avec succès.");
        } catch (Exception e) {
            logger.error("Erreur unblockIP : " + e.getMessage());
            return new Response(false, "Erreur serveur lors du déblocage.");
        }
    }
}
