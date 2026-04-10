package ma.ensate.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

public class SYNCookieManager {
    
    private static final Logger logger = LogManager.getLogger(SYNCookieManager.class);
    
    private static final int COOKIE_EXPIRY_SECONDS = 60;
    private static final String SECRET_KEY = "ChriOnline_Security_Key_2026";
    
    private final ConcurrentHashMap<String, Long> validCookies = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    public String generateCookie(InetAddress clientAddress, int clientPort) {
        String cookieData = clientAddress.getHostAddress() + ":" + clientPort + ":" + System.currentTimeMillis();
        String cookie = generateHash(cookieData);
        
        validCookies.put(cookie, System.currentTimeMillis());
        logger.info("Cookie SYN généré pour {}:{}, Total cookies actifs: {}", 
                   clientAddress.getHostAddress(), clientPort, validCookies.size());
        
        return cookie;
    }
    
    public boolean validateCookie(String cookie, InetAddress clientAddress, int clientPort) {
        Long timestamp = validCookies.get(cookie);
        
        if (timestamp == null) {
            logger.warn("Cookie invalide ou inexistant pour {}:{}, Cookies restants: {}", 
                       clientAddress.getHostAddress(), clientPort, validCookies.size());
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long expiryTime = timestamp + (COOKIE_EXPIRY_SECONDS * 1000L);
        
        if (currentTime > expiryTime) {
            validCookies.remove(cookie);
            logger.warn("Cookie expiré pour {}:{}, Cookies restants: {}", 
                       clientAddress.getHostAddress(), clientPort, validCookies.size());
            return false;
        }
        
        validCookies.remove(cookie);
        logger.info("Cookie validé avec succès pour {}:{}, Cookies restants: {}", 
                   clientAddress.getHostAddress(), clientPort, validCookies.size());
        return true;
    }
    
    public void cleanupExpiredCookies() {
        long currentTime = System.currentTimeMillis();
        long expiryTime = COOKIE_EXPIRY_SECONDS * 1000L;
        int initialCount = validCookies.size();
        
        validCookies.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > expiryTime) {
                logger.debug("Cookie expiré nettoyé");
                return true;
            }
            return false;
        });
        
        int cleanedCount = initialCount - validCookies.size();
        if (cleanedCount > 0) {
            logger.info("Nettoyage cookies terminé - {} cookies expirés supprimés, {} restants", 
                       cleanedCount, validCookies.size());
        }
    }
    
    private String generateHash(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String input = data + SECRET_KEY + random.nextInt();
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du hash du cookie", e);
            return String.valueOf(random.nextLong());
        }
    }
    
    public int getActiveCookiesCount() {
        return validCookies.size();
    }
    
    public void reset() {
        validCookies.clear();
        logger.info("SYNCookieManager réinitialisé");
    }
}
