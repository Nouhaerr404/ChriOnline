package ma.ensate.security;

import java.security.*;
import java.util.Base64;

/**
 * Signeur de challenges avec clé privée RSA
 * Utilisé côté client pour signer le challenge
 */
public class RSASigner {

    /**
     * Signe un challenge avec la clé privée RSA
     * @param challenge le challenge à signer (String)
     * @param privateKey la clé privée RSA
     * @return tableau de bytes représentant la signature
     * @throws Exception en cas d'erreur de signature
     */
    public static byte[] sign(String challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes());
        return signature.sign();
    }

    /**
     * Signe un challenge et retourne la signature encodée en Base64
     * @param challenge le challenge à signer (String)
     * @param privateKey la clé privée RSA
     * @return String représentant la signature en Base64
     * @throws Exception en cas d'erreur de signature
     */
    public static String signToBase64(String challenge, PrivateKey privateKey) throws Exception {
        byte[] signatureBytes = sign(challenge, privateKey);
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Signe un challenge avec des bytes
     * @param challengeBytes le challenge en bytes
     * @param privateKey la clé privée RSA
     * @return tableau de bytes représentant la signature
     * @throws Exception en cas d'erreur de signature
     */
    public static byte[] signBytes(byte[] challengeBytes, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challengeBytes);
        return signature.sign();
    }
}
