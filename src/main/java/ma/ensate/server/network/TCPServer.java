package ma.ensate.server.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {

    private static final Logger logger = LogManager.getLogger(TCPServer.class);
    private static final int DEFAULT_PORT = 5000;

    private ServerSocket serverSocket;
    private boolean running = false;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            logger.info("╔════════════════════════════════════════╗");
            logger.info("║     SERVEUR CHRIONLINE DÉMARRÉ        ║");
            logger.info("║     Port : " + port + "                         ║");
            logger.info("║     En attente de connexions...       ║");
            logger.info("╚════════════════════════════════════════╝");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.info("🔌 Nouveau client connecté : "
                        + clientSocket.getInetAddress().getHostAddress());

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
                logger.info(" Thread créé — clients actifs : "
                        + Thread.activeCount());
            }

        } catch (IOException e) {
            if (running) {
                logger.error(" Erreur TCPServer : " + e.getMessage());
            }
        }
    }

    public void start() {
        start(DEFAULT_PORT);
    }

    public void stop() {
        running = false;
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

    // =============================================
    // POINT D'ENTRÉE PRINCIPAL
    // =============================================
    public static void main(String[] args) {
        TCPServer server = new TCPServer();

        // ShutdownHook — arrêt propre quand on ferme le programme
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