package ma.ensate.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Chiffreur/Déchiffreur AES en mode GCM (Galois/Counter Mode).
 *
 * <p>GCM est recommandé par rapport à CBC car il fournit à la fois
 * la confidentialité ET l'authentification des données (AEAD).
 * Cela protège contre les attaques de type padding oracle et
 * garantit l'intégrité des données chiffrées.</p>
 *
 * <h3>Format du message chiffré (méthodes compact) :</h3>
 * <pre>
 * [IV (12 bytes)] || [Données chiffrées + Tag GCM (16 bytes)]
 * </pre>
 *
 * <p>L'IV est préfixé aux données chiffrées pour simplifier la transmission.
 * Cela permet au récepteur d'extraire l'IV avant le déchiffrement.</p>
 *
 * <h3>Utilisation :</h3>
 * <pre>
 * SecretKey key = AESKeyGenerator.generateKey();
 * String chiffre = AESEncryptor.encryptCompact("données sensibles", key);
 * String clair = AESEncryptor.decryptCompact(chiffre, key);
 * </pre>
 *
 * @author Personne 2
 * @see AESKeyGenerator
 */
public class AESEncryptor {

    /** Transformation AES-GCM (authentified encryption) */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** Taille du tag d'authentification GCM en bits (128 bits = maximum sécurité) */
    private static final int GCM_TAG_LENGTH = 128;

    /** Taille de l'IV pour GCM en bytes (96 bits recommandé par NIST) */
    private static final int GCM_IV_LENGTH = 12;

    // ──────────────────────────────────────────────────────────────────────
    //  Chiffrement / Déchiffrement avec IV explicite
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Chiffre des données avec AES-GCM.
     *
     * @param data les données en clair (bytes)
     * @param key  la clé secrète AES
     * @param iv   le vecteur d'initialisation (doit être unique par chiffrement)
     * @return les données chiffrées incluant le tag GCM (bytes)
     * @throws Exception en cas d'erreur de chiffrement
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public static byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        validateParameters(data, key, iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        return cipher.doFinal(data);
    }

    /**
     * Déchiffre des données chiffrées avec AES-GCM.
     *
     * @param encryptedData les données chiffrées incluant le tag GCM (bytes)
     * @param key           la clé secrète AES
     * @param iv            le vecteur d'initialisation utilisé lors du chiffrement
     * @return les données déchiffrées (bytes)
     * @throws Exception en cas d'erreur de déchiffrement ou si le tag GCM est invalide
     * @throws javax.crypto.AEADBadTagException si les données ont été altérées
     */
    public static byte[] decrypt(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        validateParameters(encryptedData, key, iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        return cipher.doFinal(encryptedData);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Chiffrement / Déchiffrement avec Base64
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Chiffre des données et retourne le résultat en Base64.
     *
     * @param data les données en clair (bytes)
     * @param key  la clé secrète AES
     * @param iv   le vecteur d'initialisation
     * @return les données chiffrées encodées en Base64
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static String encryptToBase64(byte[] data, SecretKey key, byte[] iv) throws Exception {
        byte[] encrypted = encrypt(data, key, iv);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Déchiffre des données encodées en Base64.
     *
     * @param encryptedBase64 les données chiffrées en Base64
     * @param key             la clé secrète AES
     * @param iv              le vecteur d'initialisation
     * @return les données déchiffrées (bytes)
     * @throws Exception en cas d'erreur de déchiffrement
     */
    public static byte[] decryptFromBase64(String encryptedBase64, SecretKey key, byte[] iv) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);
        return decrypt(encryptedData, key, iv);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Chiffrement / Déchiffrement de Strings
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Chiffre une String et retourne le résultat en Base64.
     *
     * @param plainText le texte en clair
     * @param key       la clé secrète AES
     * @param iv        le vecteur d'initialisation
     * @return le texte chiffré en Base64
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static String encryptString(String plainText, SecretKey key, byte[] iv) throws Exception {
        if (plainText == null) {
            throw new IllegalArgumentException("Le texte à chiffrer ne peut pas être null.");
        }
        return encryptToBase64(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8), key, iv);
    }

    /**
     * Déchiffre une String depuis sa représentation Base64.
     *
     * @param encryptedBase64 le texte chiffré en Base64
     * @param key             la clé secrète AES
     * @param iv              le vecteur d'initialisation
     * @return le texte en clair
     * @throws Exception en cas d'erreur de déchiffrement
     */
    public static String decryptString(String encryptedBase64, SecretKey key, byte[] iv) throws Exception {
        byte[] decrypted = decryptFromBase64(encryptedBase64, key, iv);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Format compact : IV préfixé aux données chiffrées
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Chiffre des données en mode compact : l'IV est généré automatiquement
     * et préfixé au résultat chiffré.
     *
     * <p>Format de sortie : {@code [IV (12 bytes)] || [ciphertext + GCM tag]}</p>
     *
     * <p>Cette méthode est la plus simple à utiliser : elle gère l'IV de manière
     * transparente. Le récepteur utilise {@link #decryptCompact(String, SecretKey)}
     * pour extraire l'IV et déchiffrer.</p>
     *
     * @param plainText le texte en clair
     * @param key       la clé secrète AES
     * @return le texte chiffré en Base64 (IV + données)
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static String encryptCompact(String plainText, SecretKey key) throws Exception {
        if (plainText == null) {
            throw new IllegalArgumentException("Le texte à chiffrer ne peut pas être null.");
        }

        byte[] iv = AESKeyGenerator.generateIV();
        byte[] encrypted = encrypt(
                plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8), key, iv);

        // Préfixer l'IV aux données chiffrées
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
        buffer.put(iv);
        buffer.put(encrypted);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Chiffre des données binaires en mode compact (IV préfixé).
     *
     * @param data les données en clair (bytes)
     * @param key  la clé secrète AES
     * @return les données chiffrées avec IV préfixé, encodées en Base64
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static String encryptCompactBytes(byte[] data, SecretKey key) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Les données ne peuvent pas être null.");
        }

        byte[] iv = AESKeyGenerator.generateIV();
        byte[] encrypted = encrypt(data, key, iv);

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
        buffer.put(iv);
        buffer.put(encrypted);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Déchiffre des données en mode compact : extrait l'IV préfixé
     * puis déchiffre les données.
     *
     * @param compactBase64 les données chiffrées en Base64 (IV + données)
     * @param key           la clé secrète AES
     * @return le texte en clair
     * @throws Exception en cas d'erreur de déchiffrement
     * @throws IllegalArgumentException si les données sont trop courtes
     */
    public static String decryptCompact(String compactBase64, SecretKey key) throws Exception {
        if (compactBase64 == null || compactBase64.isBlank()) {
            throw new IllegalArgumentException("Les données chiffrées ne peuvent pas être vides.");
        }

        byte[] decoded = Base64.getDecoder().decode(compactBase64);

        if (decoded.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException(
                "Données chiffrées trop courtes. Taille minimale : " + GCM_IV_LENGTH + " bytes.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        // Extraire l'IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        // Extraire les données chiffrées
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        byte[] decrypted = decrypt(encrypted, key, iv);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Déchiffre des données binaires en mode compact.
     *
     * @param compactBase64 les données chiffrées en Base64 (IV + données)
     * @param key           la clé secrète AES
     * @return les données déchiffrées en bytes
     * @throws Exception en cas d'erreur de déchiffrement
     */
    public static byte[] decryptCompactBytes(String compactBase64, SecretKey key) throws Exception {
        if (compactBase64 == null || compactBase64.isBlank()) {
            throw new IllegalArgumentException("Les données chiffrées ne peuvent pas être vides.");
        }

        byte[] decoded = Base64.getDecoder().decode(compactBase64);

        if (decoded.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException(
                "Données chiffrées trop courtes. Taille minimale : " + GCM_IV_LENGTH + " bytes.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        return decrypt(encrypted, key, iv);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Chiffrement avec Additional Authenticated Data (AAD)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Chiffre des données avec des données authentifiées additionnelles (AAD).
     *
     * <p>Les AAD sont des données en clair qui seront authentifiées (vérifiées
     * pour l'intégrité) mais pas chiffrées. Exemple : en-têtes de protocole,
     * identifiants de session, timestamps.</p>
     *
     * @param data les données à chiffrer
     * @param key  la clé secrète AES
     * @param iv   le vecteur d'initialisation
     * @param aad  les données authentifiées additionnelles
     * @return les données chiffrées (bytes)
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static byte[] encryptWithAAD(byte[] data, SecretKey key, byte[] iv, byte[] aad) throws Exception {
        validateParameters(data, key, iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }

        return cipher.doFinal(data);
    }

    /**
     * Déchiffre des données avec vérification des données authentifiées (AAD).
     *
     * @param encryptedData les données chiffrées (bytes)
     * @param key           la clé secrète AES
     * @param iv            le vecteur d'initialisation
     * @param aad           les données authentifiées additionnelles (doivent correspondre)
     * @return les données déchiffrées (bytes)
     * @throws Exception en cas d'erreur de déchiffrement
     * @throws javax.crypto.AEADBadTagException si les AAD ne correspondent pas
     */
    public static byte[] decryptWithAAD(byte[] encryptedData, SecretKey key, byte[] iv, byte[] aad) throws Exception {
        validateParameters(encryptedData, key, iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }

        return cipher.doFinal(encryptedData);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Validation
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Valide les paramètres de chiffrement/déchiffrement.
     *
     * @param data les données
     * @param key  la clé
     * @param iv   le vecteur d'initialisation
     * @throws IllegalArgumentException si un paramètre est invalide
     */
    private static void validateParameters(byte[] data, SecretKey key, byte[] iv) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Les données ne peuvent pas être null ou vides.");
        }
        if (key == null) {
            throw new IllegalArgumentException("La clé AES ne peut pas être null.");
        }
        if (iv == null || iv.length == 0) {
            throw new IllegalArgumentException("L'IV ne peut pas être null ou vide.");
        }
    }
}
