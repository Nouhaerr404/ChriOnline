package ma.ensate.server.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Serveur {

    private static final Logger logger = LogManager.getLogger(Serveur.class);
    private static final int PORT = 5000;

    public static void main(String[] args) {
        logger.info(" Démarrage du serveur ChriOnline sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info(" Serveur prêt — en attente de clients...");

            while (true) {
                // Attendre une connexion client
                Socket clientSocket = serverSocket.accept();
                logger.info(" Nouveau client connecté : "
                        + clientSocket.getInetAddress().getHostAddress());

                // Créer un thread pour ce client
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
                logger.info(" Thread créé — clients actifs : "
                        + Thread.activeCount());
            }

        } catch (IOException e) {
            logger.error(" Erreur serveur : " + e.getMessage());
        }
    }
}