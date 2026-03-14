package ma.ensate.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serveur TCP principal pour l'application ChriOnline
 * Écoute les connexions entrantes et crée un ClientHandler pour chaque client
 */
public class TCPServer {
    
    private static final int DEFAULT_PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running = false;

    /**
     * Démarre le serveur TCP
     */
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║          SERVEUR TCP CHRIONLINE DÉMARRÉ                   ║");
            System.out.println("║          Port: " + port + "                                      ║");
            System.out.println("║          En attente de connexions...                     ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println();

            // Boucle principale d'acceptation des connexions
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion: " + clientSocket.getInetAddress());
                
                // Créer un handler pour ce client dans un thread séparé
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
            
        } catch (IOException e) {
            if (running) {
                System.err.println("Erreur du serveur: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Démarre le serveur sur le port par défaut
     */
    public void start() {
        start(DEFAULT_PORT);
    }

    /**
     * Arrête le serveur
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Serveur arrêté.");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du serveur: " + e.getMessage());
        }
    }

    /**
     * Point d'entrée principal du serveur
     */
    public static void main(String[] args) {
        TCPServer server = new TCPServer();
        
        // Gérer l'arrêt propre du serveur
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nArrêt du serveur...");
            server.stop();
        }));
        
        // Démarrer le serveur
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port par défaut: " + DEFAULT_PORT);
            }
        }
        
        server.start(port);
    }
}

