package ma.ensate.server.network;

import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.PaiementRequest;
import ma.ensate.server.dao.UtilisateurDAO;
import ma.ensate.server.services.PaymentService;
import ma.ensate.server.services.SessionManager;
import ma.ensate.util.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Petit serveur TLS reserve au paiement.
 */
public class TLSPaymentServer implements Runnable {

    private static final Logger logger = LogManager.getLogger(TLSPaymentServer.class);

    private static final int DEFAULT_PORT = ConfigLoader.getInt("TLS_PAYMENT_PORT", 9999);
    private static final String KEYSTORE_PATH =
            ConfigLoader.get("TLS_KEYSTORE_PATH", "tls/server-keystore.jks");
    private static final String KEYSTORE_PASSWORD =
            ConfigLoader.get("TLS_KEYSTORE_PASSWORD", "123456");

    private final PaymentService paymentService = new PaymentService();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    private volatile boolean running;
    private SSLServerSocket serverSocket;

    @Override
    public void run() {
        File keyStore = new File(KEYSTORE_PATH);
        if (!keyStore.exists()) {
            logger.warn("TLS paiement desactive: keystore introuvable ({})", keyStore.getAbsolutePath());
            return;
        }

        try {
            System.setProperty("javax.net.ssl.keyStore", keyStore.getAbsolutePath());
            System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);

            SSLServerSocketFactory factory =
                    (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            serverSocket = (SSLServerSocket) factory.createServerSocket(DEFAULT_PORT);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            serverSocket.setNeedClientAuth(false);
            running = true;

            logger.info("Serveur TLS de paiement demarre sur le port {}", DEFAULT_PORT);

            while (running) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                Thread thread = new Thread(() -> handleClient(clientSocket), "tls-payment-client");
                thread.start();
            }
        } catch (Exception e) {
            if (running) {
                logger.error("Erreur serveur TLS paiement : {}", e.getMessage());
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            logger.error("Erreur arret serveur TLS paiement : {}", e.getMessage());
        }
    }

    private void handleClient(SSLSocket socket) {
        String clientIP = socket.getInetAddress().getHostAddress();

        try (SSLSocket clientSocket = socket) {
            clientSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            clientSocket.startHandshake();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                String paymentLine = in.readLine();
                logger.info("Paiement TLS recu: {}", paymentLine);

                ParsedPaymentRequest parsedRequest = parsePaymentLine(paymentLine);
                if (parsedRequest == null) {
                    out.println(buildResponseLine(new Response(false, "Requete TLS de paiement invalide")));
                    return;
                }

                SessionManager.SessionResult sessionResult =
                        SessionManager.evaluerEtRegenerer(parsedRequest.token(), clientIP);

                if (!sessionResult.isValid) {
                    out.println(buildResponseLine(new Response(false, sessionResult.errorMessage)));
                    return;
                }

                Response response = paymentService.traiterPaiement(parsedRequest.paiementRequest());

                if (sessionResult.latestToken != null
                        && !sessionResult.latestToken.equals(parsedRequest.token())) {
                    response.setNewToken(sessionResult.latestToken);
                    updateStoredToken(parsedRequest.token(), sessionResult.latestToken);
                }

                out.println(buildResponseLine(response));
            }
        } catch (Exception e) {
            logger.error("Erreur client TLS paiement ({}): {}", clientIP, e.getMessage());
        }
    }

    private ParsedPaymentRequest parsePaymentLine(String paymentLine) {
        if (paymentLine == null || paymentLine.isBlank()) {
            return null;
        }

        Map<String, String> values = parseLine(paymentLine);
        String commandeId = values.get("commandeId");
        String methode = values.get("methode");
        String cardLast4 = values.getOrDefault("cardLast4", "");
        String token = values.get("token");

        if (commandeId == null || methode == null || token == null) {
            return null;
        }

        PaiementRequest paiementRequest = new PaiementRequest(commandeId, methode, cardLast4);
        return new ParsedPaymentRequest(paiementRequest, token);
    }

    private String buildResponseLine(Response response) {
        return "status=" + (response.isSuccess() ? "OK" : "ERROR")
                + ";message=" + encode(response.getMessage())
                + ";newToken=" + encode(response.getNewToken() == null ? "" : response.getNewToken());
    }

    private Map<String, String> parseLine(String line) {
        Map<String, String> values = new HashMap<>();
        String[] entries = line.split(";");
        for (String entry : entries) {
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = entry.substring(0, separator);
            String value = decode(entry.substring(separator + 1));
            values.put(key, value);
        }
        return values;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void updateStoredToken(String oldToken, String newToken) {
        try {
            Utilisateur utilisateur = utilisateurDAO.trouverParToken(oldToken);
            if (utilisateur != null) {
                utilisateurDAO.sauvegarderToken(utilisateur.getId(), newToken);
            }
        } catch (Exception e) {
            logger.error("Erreur mise a jour token TLS paiement : {}", e.getMessage());
        }
    }

    private record ParsedPaymentRequest(PaiementRequest paiementRequest, String token) {}
}
