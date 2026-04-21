package ma.ensate.server.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SYNFloodProtection {
    
    private static final Logger logger = LogManager.getLogger(SYNFloodProtection.class);
    
    private static final int MAX_CONNECTIONS_PER_IP = 100;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int MAX_PENDING_CONNECTIONS = 10000;
    
    private final ConcurrentHashMap<String, AtomicInteger> ipConnectionCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> pendingConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalPendingConnections = new AtomicInteger(0);
    
    public boolean allowConnection(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        
        logger.info("Nouvelle connexion demandée - IP: {}, Connexions en attente: {}/{}", 
                   ip, totalPendingConnections.get(), MAX_PENDING_CONNECTIONS);
        
        if (isIPRateLimited(ip)) {
            logger.warn("IP {} bloquée - limite de connexions dépassée ({}/{})", 
                       ip, ipConnectionCounts.get(ip).get(), MAX_CONNECTIONS_PER_IP);
            return false;
        }
        
        if (isMaxPendingReached()) {
            logger.warn("Limite de connexions en attente atteinte ({}/{}) - refus de nouvelle connexion", 
                       totalPendingConnections.get(), MAX_PENDING_CONNECTIONS);
            return false;
        }
        
        recordPendingConnection(ip);
        logger.info("Connexion autorisée pour IP: {}, Total en attente: {}", 
                   ip, totalPendingConnections.get());
        return true;
    }
    
    public void confirmConnection(InetAddress clientAddress) {
        String ip = clientAddress.getHostAddress();
        pendingConnections.remove(ip);
        totalPendingConnections.decrementAndGet();
        logger.info("Connexion confirmée pour IP: {}, Restant en attente: {}", 
                   ip, totalPendingConnections.get());
    }
    
    public void cleanupExpiredConnections() {
        long currentTime = System.currentTimeMillis();
        long timeoutMillis = CONNECTION_TIMEOUT_SECONDS * 1000L;
        int initialCount = pendingConnections.size();
        
        pendingConnections.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > timeoutMillis) {
                String ip = entry.getKey();
                ipConnectionCounts.computeIfPresent(ip, (key, count) -> {
                    int newCount = count.decrementAndGet();
                    return newCount <= 0 ? null : count;
                });
                totalPendingConnections.decrementAndGet();
                logger.info("Connexion expirée nettoyée pour IP: {}", ip);
                return true;
            }
            return false;
        });
        
        int cleanedCount = initialCount - pendingConnections.size();
        if (cleanedCount > 0) {
            logger.info("Nettoyage terminé - {} connexions expirées supprimées, {} restantes", 
                       cleanedCount, pendingConnections.size());
        }
    }
    
    private boolean isIPRateLimited(String ip) {
        AtomicInteger count = ipConnectionCounts.get(ip);
        return count != null && count.get() >= MAX_CONNECTIONS_PER_IP;
    }
    
    private boolean isMaxPendingReached() {
        return totalPendingConnections.get() >= MAX_PENDING_CONNECTIONS;
    }
    
    private void recordPendingConnection(String ip) {
        ipConnectionCounts.compute(ip, (key, count) -> {
            if (count == null) {
                return new AtomicInteger(1);
            } else {
                count.incrementAndGet();
                return count;
            }
        });
        
        pendingConnections.put(ip, System.currentTimeMillis());
        totalPendingConnections.incrementAndGet();
    }
    
    public int getPendingConnectionsCount() {
        return totalPendingConnections.get();
    }
    
    public int getActiveIPCount() {
        return ipConnectionCounts.size();
    }
    
    public void reset() {
        ipConnectionCounts.clear();
        pendingConnections.clear();
        totalPendingConnections.set(0);
        logger.info("SYN Flood protection réinitialisée");
    }
}
