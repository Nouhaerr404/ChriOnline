package ma.ensate.server.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ma.ensate.util.ConfigLoader;
import ma.ensate.server.security.SYNFloodProtection;
import ma.ensate.server.security.SYNCookieManager;

public class TCPServer {

    private static final Logger logger = LogManager.getLogger(TCPServer.class);
    private static final int DEFAULT_PORT = ConfigLoader.getInt("SERVER_PORT", 5000);

    private ServerSocket serverSocket;
    private boolean running = false;
    
    private final SYNFloodProtection synFloodProtection = new SYNFloodProtection();
    private final SYNCookieManager synCookieManager = new SYNCookieManager();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            startCleanupScheduler();

            logger.info("     SERVEUR CHRIONLINE DÉMARRÉ        ");
            logger.info("     Port : " + port + "                         ");
            logger.info("     Protection SYN Flood activée    ");
            logger.info("     En attente de connexions...       ");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                
                if (!synFloodProtection.allowConnection(clientSocket.getInetAddress())) {
                    logger.warn("Connexion rejetée - protection SYN Flood: {}", 
                            clientSocket.getInetAddress().getHostAddress());
                    clientSocket.close();
                    continue;
                }
                
                logger.info(" Nouveau client connecté : "
                        + clientSocket.getInetAddress().getHostAddress());

                Thread t = new Thread(new ClientHandler(clientSocket, synFloodProtection, synCookieManager));
                t.start();
                logger.info(" Thread créé - clients actifs : "
                        + Thread.activeCount());
            }

        } catch (IOException e) {
            logger.error("Erreur lors du démarrage du TCPServer : " + e.getMessage());
        }
    }

    public void start() {
        start(DEFAULT_PORT);
    }

    private void startCleanupScheduler() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            synFloodProtection.cleanupExpiredConnections();
            synCookieManager.cleanupExpiredCookies();
            
            logger.info("Nettoyage périodique - Connexions en attente: {}, Cookies actifs: {}", 
                    synFloodProtection.getPendingConnectionsCount(),
                    synCookieManager.getActiveCookiesCount());
        }, 30, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        running = false;
        cleanupExecutor.shutdown();
        
        try {
            if (cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.info("Cleanup scheduler arrêté proprement");
            } else {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                logger.info("TCPServer arrêté proprement.");
            }
        } catch (IOException e) {
            logger.error("Erreur lors de l'arrêt du TCPServer : "
                    + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TCPServer server = new TCPServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info(" Arrêt du TCPServer...");
            server.stop();
        }));

        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Port invalide, utilisation du port par défaut : "
                        + DEFAULT_PORT);
            }
        }

        server.start(port);
    }
}
