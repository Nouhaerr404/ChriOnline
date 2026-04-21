package ma.ensate.client.security;

import ma.ensate.client.network.ClientTCP;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.security.RSASigner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.PrivateKey;

/**
 * Client-side utility for admin challenge-response authentication
 */
public class AdminAuthClient {

    private static final Logger logger = LogManager.getLogger(AdminAuthClient.class);

    /**
     * Request a challenge from the server for admin authentication
     * @param adminEmail the admin email
     * @return the challenge string, or null if failed
     */
    public static String requestChallenge(String adminEmail) {
        try {
            Request request = new Request("GENERATE_CHALLENGE_ADMIN", adminEmail);
            Response response = ClientTCP.getInstance().envoyerRequete(request);

            if (response.isSuccess()) {
                String challenge = (String) response.getData();
                logger.info("Challenge reçu pour admin : " + adminEmail);
                return challenge;
            } else {
                logger.warn("Échec génération challenge : " + response.getMessage());
                return null;
            }
        } catch (Exception e) {
            logger.error("Erreur requestChallenge : " + e.getMessage());
            return null;
        }
    }

    /**
     * Sign the challenge with the private key and send to server for verification
     * @param adminEmail the admin email
     * @param challenge the challenge received from server
     * @param privateKey the admin's private key
     * @return true if authentication succeeded
     */
    public static boolean authenticateWithChallenge(String adminEmail, String challenge, PrivateKey privateKey) {
        try {
            // Sign the challenge
            String signatureBase64 = RSASigner.signToBase64(challenge, privateKey);
            logger.info("Challenge signé pour admin : " + adminEmail);

            // Send to server for verification
            Object[] payload = {adminEmail, challenge, signatureBase64};
            Request request = new Request("VERIFY_SIGNATURE_ADMIN", payload);
            Response response = ClientTCP.getInstance().envoyerRequete(request);

            if (response.isSuccess()) {
                logger.info("Authentification admin réussie par challenge-response : " + adminEmail);
                return true;
            } else {
                logger.warn("Échec authentification admin : " + response.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("Erreur authenticateWithChallenge : " + e.getMessage());
            return false;
        }
    }

    /**
     * Complete authentication flow: request challenge, sign it, and verify
     * @param adminEmail the admin email
     * @param privateKey the admin's private key
     * @return true if authentication succeeded
     */
    public static boolean authenticate(String adminEmail, PrivateKey privateKey) {
        String challenge = requestChallenge(adminEmail);
        if (challenge == null) {
            return false;
        }
        return authenticateWithChallenge(adminEmail, challenge, privateKey);
    }
}
