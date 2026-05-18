package ma.ensate.server.security.ips;

import ma.ensate.server.dao.DBConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;

public class IPSManager {
    private static final Logger logger = LogManager.getLogger(IPSManager.class);
    
    // Cache en mémoire pour un accès rapide lors de la vérification de chaque requête
    private static final ConcurrentHashMap<String, Long> blockedIPsCache = new ConcurrentHashMap<>();

    // Appelé au démarrage du serveur pour charger les blocages existants depuis la BDD
    public static void loadBlockedIPsFromDB() {
        String sql = "SELECT ip_address, blocked_until FROM blocked_ips WHERE blocked_until > NOW()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                blockedIPsCache.put(rs.getString("ip_address"), rs.getTimestamp("blocked_until").getTime());
            }
            logger.info("Chargement de {} IPs bloquees depuis la BDD.", blockedIPsCache.size());
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des IPs bloquées : {}", e.getMessage());
        }
    }

    public static boolean isIPBlocked(String ipAddress) {
        Long blockedUntil = blockedIPsCache.get(ipAddress);
        if (blockedUntil == null) return false;

        if (System.currentTimeMillis() > blockedUntil) {
            // Le blocage a expiré, on l'enlève du cache
            blockedIPsCache.remove(ipAddress);
            return false;
        }
        return true;
    }

    public static void blockIP(String ipAddress, int durationMinutes, String reason) {
        long unblockTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        blockedIPsCache.put(ipAddress, unblockTime);
        
        logger.warn("🛑 IPS: Blocage de l'IP {} pour {} minutes. Raison : {}", ipAddress, durationMinutes, reason);

        // Sauvegarder en BDD
        String sql = "INSERT INTO blocked_ips (ip_address, blocked_until, reason) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE blocked_until = ?, reason = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            Timestamp ts = new Timestamp(unblockTime);
            pstmt.setString(1, ipAddress);
            pstmt.setTimestamp(2, ts);
            pstmt.setString(3, reason);
            pstmt.setTimestamp(4, ts);
            pstmt.setString(5, reason);
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde du blocage IP en BDD : {}", e.getMessage());
        }
    }

    public static void unblockIP(String ipAddress) {
        blockedIPsCache.remove(ipAddress);
        String sql = "DELETE FROM blocked_ips WHERE ip_address = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.executeUpdate();
            logger.info("✅ IPS: Déblocage manuel de l'IP {}", ipAddress);
        } catch (Exception e) {
            logger.error("Erreur lors du déblocage IP en BDD : {}", e.getMessage());
        }
    }
}
