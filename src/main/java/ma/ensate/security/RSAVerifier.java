package ma.ensate.security;

import java.security.*;
import java.util.Base64;

/**
 * Vérificateur de signatures avec clé publique RSA
 * Utilisé côté serveur pour vérifier la signature du challenge
 */
public class RSAVerifier {

    /**
     * Vérifie la signature d'un challenge avec la clé publique RSA
     * @param challenge le challenge original (String)
     * @param signatureBytes la signature en bytes
     * @param publicKey la clé publique RSA
     * @return true si la signature est valide, false sinon
     * @throws Exception en cas d'erreur de vérification
     */
    public static boolean verify(String challenge, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge.getBytes());
        return signature.verify(signatureBytes);
    }

    /**
     * Vérifie la signature encodée en Base64
     * @param challenge le challenge original (String)
     * @param signatureBase64 la signature encodée en Base64
     * @param publicKey la clé publique RSA
     * @return true si la signature est valide, false sinon
     * @throws Exception en cas d'erreur de vérification
     */
    public static boolean verifyBase64(String challenge, String signatureBase64, PublicKey publicKey) throws Exception {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return verify(challenge, signatureBytes, publicKey);
    }

    /**
     * Vérifie la signature avec des bytes
     * @param challengeBytes le challenge en bytes
     * @param signatureBytes la signature en bytes
     * @param publicKey la clé publique RSA
     * @return true si la signature est valide, false sinon
     * @throws Exception en cas d'erreur de vérification
     */
    public static boolean verifyBytes(byte[] challengeBytes, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challengeBytes);
        return signature.verify(signatureBytes);
    }
}
