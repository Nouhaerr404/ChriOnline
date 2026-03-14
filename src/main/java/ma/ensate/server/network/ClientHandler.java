package ma.ensate.server.network;

import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.server.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket socket;

    // Actions qui ne nécessitent PAS de token
    private static final java.util.Set<String> ACTIONS_PUBLIQUES =
            new java.util.HashSet<>(java.util.Arrays.asList(
                    "LOGIN",
                    "REGISTER"
            ));

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String clientIP = socket.getInetAddress().getHostAddress();
        logger.info("Handler démarré pour : " + clientIP);

        try (
                ObjectOutputStream out = new ObjectOutputStream(
                        socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(
                        socket.getInputStream())
        ) {
            while (true) {
                // 1. Recevoir la requête
                Request request = (Request) in.readObject();
                logger.info(" Action reçue : " + request.getAction()
                        + " | Client : " + clientIP);

                // 2. Vérifier le token si action protégée (TP5)
                if (!ACTIONS_PUBLIQUES.contains(request.getAction())) {
                    String token = request.getToken();
                    if (!UserService.verifierToken(token)) {
                        logger.warn(" Accès refusé — token invalide"
                                + " | Action : " + request.getAction()
                                + " | Client : " + clientIP);
                        out.writeObject(new Response(false,
                                "Non autorisé. Veuillez vous connecter."));
                        out.flush();
                        continue;
                    }
                }

                // 3. Router vers le bon service
                Response response = traiterRequete(request);

                // 4. Envoyer la réponse
                out.writeObject(response);
                out.flush();
            }

        } catch (EOFException e) {
            // Client déconnecté normalement
            logger.info(" Client déconnecté : " + clientIP);
        } catch (Exception e) {
            logger.error(" Erreur handler " + clientIP
                    + " : " + e.getMessage());
        }
    }

    private Response traiterRequete(Request request) {
        switch (request.getAction()) {

            // ── Personne 1 — Sécurité ──────────────────
            case "LOGIN":
                return UserService.login(request.getData());

            case "REGISTER":
                return UserService.register(request.getData());

            case "LOGOUT":
                return UserService.logout(request.getData());

            // ── Personne 2 — Produits ──────────────────
            case "GET_ALL_PRODUCTS":
            case "GET_PRODUCT_BY_ID":
            case "GET_BY_CATEGORY":
            case "GET_ALL_CATEGORIES":
                // TODO : Personne 2 ajoutera ses services ici
                return new Response(false, "Non implémenté encore.");

            // ── Personne 3 — Panier ────────────────────
            case "GET_CART":
            case "ADD_TO_CART":
            case "REMOVE_FROM_CART":
            case "UPDATE_CART":
            case "CLEAR_CART":
                // TODO : Personne 3 ajoutera ses services ici
                return new Response(false, "Non implémenté encore.");

            // ── Personne 4 — Commandes ─────────────────
            case "PLACE_ORDER":
            case "PROCESS_PAYMENT":
            case "GET_ORDER_HISTORY":
            case "GET_ORDER_BY_ID":
            case "UPDATE_ORDER_STATUS":
                // TODO : Personne 4 ajoutera ses services ici
                return new Response(false, "Non implémenté encore.");

            default:
                logger.warn(" Action inconnue : " + request.getAction());
                return new Response(false, "Action inconnue.");
        }
    }
}