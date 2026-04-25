package ma.ensate.client.network;

import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.PaiementRequest;
import ma.ensate.util.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Client TLS dedie a la phase de paiement.
 */
public class TLSPaymentClient {

    private static final Logger logger = LogManager.getLogger(TLSPaymentClient.class);

    private static final String HOST = ConfigLoader.get("SERVER_HOST", "localhost");
    private static final int PORT = ConfigLoader.getInt("TLS_PAYMENT_PORT", 9999);
    private static final String TRUSTSTORE_PATH =
            ConfigLoader.get("TLS_TRUSTSTORE_PATH", "tls/client-truststore.jks");
    private static final String TRUSTSTORE_PASSWORD =
            ConfigLoader.get("TLS_TRUSTSTORE_PASSWORD", "123456");

    private TLSPaymentClient() {}

    public static Response effectuerPaiement(PaiementRequest paiementRequest, String token) throws Exception {
        File trustStore = new File(TRUSTSTORE_PATH);
        if (!trustStore.exists()) {
            throw new IllegalStateException(
                    "TrustStore introuvable: " + trustStore.getAbsolutePath());
        }

        System.setProperty("javax.net.ssl.trustStore", trustStore.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);

        SSLSocketFactory factory = context.getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(HOST, PORT)) {
            socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            socket.startHandshake();

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String payload = buildPaymentLine(paiementRequest, token);
                out.println(payload);

                String responseLine = in.readLine();
                Response response = parseResponseLine(responseLine);
                SessionManager.getInstance().updateToken(response.getNewToken());
                logger.info("Reponse paiement TLS recue : {}", response.getMessage());
                return response;
            }
        }
    }

    private static String buildPaymentLine(PaiementRequest paiementRequest, String token) {
        return "commandeId=" + encode(paiementRequest.getCommandeId())
                + ";methode=" + encode(paiementRequest.getMethodePaiement())
                + ";cardLast4=" + encode(paiementRequest.getCardLast4() == null ? "" : paiementRequest.getCardLast4())
                + ";token=" + encode(token == null ? "" : token);
    }

    private static Response parseResponseLine(String responseLine) {
        if (responseLine == null || responseLine.isBlank()) {
            return new Response(false, "Reponse vide du serveur TLS.");
        }

        Map<String, String> values = parseLine(responseLine);
        boolean success = "OK".equalsIgnoreCase(values.getOrDefault("status", "ERROR"));
        Response response = new Response(success, values.getOrDefault("message", "Reponse TLS inconnue."));
        response.setNewToken(values.get("newToken"));
        return response;
    }

    private static Map<String, String> parseLine(String line) {
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

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
