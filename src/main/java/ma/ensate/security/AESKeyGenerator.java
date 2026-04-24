package ma.ensate.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Générateur de clés AES pour le chiffrement symétrique.
 * Génère des clés AES-256 de manière sécurisée avec {@link SecureRandom}.
 *
 * <p>Utilisé dans le protocole HTTPS-like de ChriOnline :
 * le client génère une clé AES, la chiffre avec RSA, et l'envoie au serveur
 * pour établir un canal de communication sécurisé.</p>
 *
 * @author Personne 2
 * @see AESEncryptor
 */
public class AESKeyGenerator {

    /** Algorithme utilisé */
    private static final String ALGORITHM = "AES";

    /** Taille de la clé en bits (AES-256) */
    private static final int KEY_SIZE = 256;

    /** Taille de l'IV pour AES-GCM en bytes (96 bits recommandé par le NIST) */
    private static final int GCM_IV_LENGTH = 12;

    /** Source d'aléa cryptographiquement sûre */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Génère une nouvelle clé AES-256 de manière sécurisée.
     *
     * @return la clé secrète AES-256 générée
     * @throws NoSuchAlgorithmException si l'algorithme AES n'est pas disponible
     */
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, SECURE_RANDOM);
        return keyGenerator.generateKey();
    }

    /**
     * Génère une nouvelle clé AES avec une taille personnalisée.
     *
     * @param keySize la taille de la clé en bits (128, 192 ou 256)
     * @return la clé secrète AES générée
     * @throws NoSuchAlgorithmException si l'algorithme AES n'est pas disponible
     * @throws IllegalArgumentException si la taille de clé n'est pas valide
     */
    public static SecretKey generateKey(int keySize) throws NoSuchAlgorithmException {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new IllegalArgumentException(
                "Taille de clé AES invalide : " + keySize + ". Valeurs acceptées : 128, 192, 256.");
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(keySize, SECURE_RANDOM);
        return keyGenerator.generateKey();
    }

    /**
     * Génère un vecteur d'initialisation (IV) unique pour AES-GCM.
     * Le NIST recommande un IV de 96 bits (12 bytes) pour GCM.
     *
     * <p>Un IV unique DOIT être utilisé pour chaque opération de chiffrement
     * avec la même clé. La réutilisation d'un IV compromet la sécurité.</p>
     *
     * @return un IV de 12 bytes généré de manière sécurisée
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Génère un IV avec une taille personnalisée.
     *
     * @param length la taille de l'IV en bytes
     * @return un IV de la taille spécifiée
     * @throws IllegalArgumentException si la taille est invalide
     */
    public static byte[] generateIV(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("La taille de l'IV doit être positive.");
        }
        byte[] iv = new byte[length];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Sérialise une clé AES en Base64 pour transmission ou stockage.
     *
     * @param key la clé AES à sérialiser
     * @return la clé encodée en Base64
     */
    public static String serializeKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Désérialise une clé AES depuis sa représentation Base64.
     *
     * @param base64Key la clé encodée en Base64
     * @return la clé secrète AES reconstruite
     * @throws IllegalArgumentException si la clé Base64 est invalide
     */
    public static SecretKey deserializeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("La clé Base64 ne peut pas être vide.");
        }
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * Encode un IV en Base64 pour transmission.
     *
     * @param iv le vecteur d'initialisation
     * @return l'IV encodé en Base64
     */
    public static String encodeIV(byte[] iv) {
        return Base64.getEncoder().encodeToString(iv);
    }

    /**
     * Décode un IV depuis sa représentation Base64.
     *
     * @param base64IV l'IV encodé en Base64
     * @return l'IV en bytes
     */
    public static byte[] decodeIV(String base64IV) {
        if (base64IV == null || base64IV.isBlank()) {
            throw new IllegalArgumentException("L'IV Base64 ne peut pas être vide.");
        }
        return Base64.getDecoder().decode(base64IV);
    }

    /**
     * Retourne la taille par défaut de la clé AES (en bits).
     *
     * @return 256 (AES-256)
     */
    public static int getDefaultKeySize() {
        return KEY_SIZE;
    }

    /**
     * Retourne la taille par défaut de l'IV pour GCM (en bytes).
     *
     * @return 12 (96 bits)
     */
    public static int getDefaultIVLength() {
        return GCM_IV_LENGTH;
    }
}
