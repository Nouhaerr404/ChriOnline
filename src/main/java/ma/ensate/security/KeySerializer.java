package ma.ensate.security;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utilitaires pour sérialiser et désérialiser les clés RSA
 * Permet de stocker les clés en Base64 dans la base de données
 */
public class KeySerializer {

    /**
     * Sérialise une clé publique en Base64
     * @param publicKey la clé publique à sérialiser
     * @return String représentant la clé en Base64 (format X.509)
     */
    public static String serializePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Sérialise une clé privée en Base64
     * @param privateKey la clé privée à sérialiser
     * @return String représentant la clé en Base64 (format PKCS#8)
     */
    public static String serializePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Désérialise une clé publique depuis Base64
     * @param base64Key la clé encodée en Base64 (format X.509)
     * @return PublicKey désérialisée
     * @throws Exception en cas d'erreur de désérialisation
     */
    public static PublicKey deserializePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Désérialise une clé privée depuis Base64
     * @param base64Key la clé encodée en Base64 (format PKCS#8)
     * @return PrivateKey désérialisée
     * @throws Exception en cas d'erreur de désérialisation
     */
    public static PrivateKey deserializePrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return keyFactory.generatePrivate(keySpec);
    }
}
