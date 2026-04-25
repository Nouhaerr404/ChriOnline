package ma.ensate.client.network;

import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import ma.ensate.util.ConfigLoader;
import ma.ensate.security.SecureHandshake;
import ma.ensate.security.SecureChannel;
import ma.ensate.protocol.dto.HandshakeRequest;
import ma.ensate.protocol.dto.HandshakeResponse;

public class ClientTCP {

    private static final Logger logger = LogManager.getLogger(ClientTCP.class);

    private static final String HOST = ConfigLoader.get("SERVER_HOST", "localhost");
    private static final int    PORT = ConfigLoader.getInt("SERVER_PORT", 5000);

    private static ClientTCP instance;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private SecureHandshake handshake;
    private SecureChannel secureChannel;

    private ClientTCP() {}

    public static ClientTCP getInstance() {
        if (instance == null) {
            instance = new ClientTCP();
        }
        return instance;
    }

    public void connecter() throws Exception {
        socket = new Socket(HOST, PORT);
        out    = new ObjectOutputStream(socket.getOutputStream());
        in     = new ObjectInputStream(socket.getInputStream());
        logger.info("Connecte au serveur " + HOST + ":" + PORT);

        try {
            System.out.println("[CLIENT] Initiation handshake...");

            // 1. Créer handshake client
            this.handshake = new SecureHandshake(true);  // true = client mode

            // 2. Demander clé publique du serveur
            HandshakeRequest req1 = handshake.initiateHandshake();
            out.writeObject(req1);
            out.flush();
            System.out.println("[CLIENT] Demande clé publique envoyée");

            // 3. Recevoir clé publique
            HandshakeResponse resp = (HandshakeResponse) in.readObject();
            System.out.println("[CLIENT] Clé publique reçue");

            // 4. Envoyer clé AES chiffrée
            HandshakeRequest req2 = handshake.sendEncryptedAESKey(resp, req1.getNonce());
            out.writeObject(req2);
            out.flush();
            System.out.println("[CLIENT] Clé AES chiffrée envoyée");

            // 5. Recevoir confirmation
            HandshakeResponse okResp = (HandshakeResponse) in.readObject();
            if (!"HANDSHAKE_COMPLETE".equals(okResp.getPhase())) {
                throw new Exception("Handshake échoué!");
            }
            System.out.println("[CLIENT] Handshake confirmé par serveur");

            // 6. Créer canal sécurisé
            this.secureChannel = new SecureChannel(
                    handshake.getNegotiatedAESKey(),
                    in,
                    out
            );

            System.out.println("[CLIENT] Canal sécurisé établi");

        } catch (Exception e) {
            logger.error("Erreur handshake: " + e.getMessage());
            socket.close();
            throw new Exception("Handshake échoué", e);
        }
    }

    public synchronized Response envoyerRequete(Request request) throws Exception {
        if (socket == null || socket.isClosed()) {
            connecter();
        }

        // Utiliser SecureChannel pour chiffrer/déchiffrer
        secureChannel.writeSecureRequest(request);

        Response response = secureChannel.readSecureResponse();
        SessionManager.getInstance().updateToken(response.getNewToken());
        logger.info(" Reponse recue : " + response.getMessage());
        return response;
    }

    public Response envoyerRequeteSecurisee(String action,
                                            Object data) throws Exception {

        String token = SessionManager.getInstance().getToken();

        Request request = new Request(action, data, token);
        return envoyerRequete(request);
    }

    public void deconnecter() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.info(" Deconnecte du serveur.");
            }
        } catch (IOException e) {
            logger.error("Erreur deconnexion : " + e.getMessage());
        }
    }

    public boolean estConnecte() {
        return socket != null && !socket.isClosed();
    }
}
