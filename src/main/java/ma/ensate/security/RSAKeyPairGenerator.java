package ma.ensate.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Générateur de paires de clés RSA pour l'authentification admin
 */
public class RSAKeyPairGenerator {

    private static final int KEY_SIZE = 2048;

    /**
     * Génère une paire de clés RSA (publique/privée)
     * @return KeyPair contenant la clé publique et privée
     * @throws NoSuchAlgorithmException si RSA n'est pas disponible
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_SIZE);
        return generator.generateKeyPair();
    }

    /**
     * Génère une paire de clés RSA avec une taille personnalisée
     * @param keySize taille de la clé en bits (1024, 2048, 4096)
     * @return KeyPair contenant la clé publique et privée
     * @throws NoSuchAlgorithmException si RSA n'est pas disponible
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }
}
