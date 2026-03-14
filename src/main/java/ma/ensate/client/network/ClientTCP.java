package ma.ensate.client.network;

import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class ClientTCP {

    private static final Logger logger = LogManager.getLogger(ClientTCP.class);

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    // Singleton — une seule connexion pour toute l'app
    private static ClientTCP instance;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // Constructeur privé → Singleton
    private ClientTCP() {}

    public static ClientTCP getInstance() {
        if (instance == null) {
            instance = new ClientTCP();
        }
        return instance;
    }

    // =============================================
    // CONNEXION AU SERVEUR
    // =============================================
    public void connecter() throws IOException {
        socket = new Socket(HOST, PORT);
        out    = new ObjectOutputStream(socket.getOutputStream());
        in     = new ObjectInputStream(socket.getInputStream());
        logger.info("✅ Connecté au serveur " + HOST + ":" + PORT);
    }

    // =============================================
    // ENVOYER UNE REQUÊTE — SANS TOKEN
    // (LOGIN et REGISTER uniquement)
    // =============================================
    public synchronized Response envoyerRequete(Request request) throws Exception {
        // Reconnexion automatique si besoin
        if (socket == null || socket.isClosed()) {
            connecter();
        }

        out.writeObject(request);
        out.flush();

        Response response = (Response) in.readObject();
        logger.info("📨 Réponse reçue : " + response.getMessage());
        return response;
    }

    // =============================================
    // ENVOYER UNE REQUÊTE — AVEC TOKEN
    // (toutes les actions protégées)
    // =============================================
    public Response envoyerRequeteSecurisee(String action,
                                            Object data) throws Exception {
        // Récupérer le token depuis SessionManager
        String token = SessionManager.getInstance().getToken();

        Request request = new Request(action, data, token);
        return envoyerRequete(request);
    }

    // =============================================
    // DÉCONNEXION
    // =============================================
    public void deconnecter() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.info("🔌 Déconnecté du serveur.");
            }
        } catch (IOException e) {
            logger.error("Erreur déconnexion : " + e.getMessage());
        }
    }

    public boolean estConnecte() {
        return socket != null && !socket.isClosed();
    }
}
