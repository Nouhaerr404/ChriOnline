package ma.ensate.security;

import ma.ensate.util.ConfigLoader;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Chiffrement AES des donnees sensibles stockees en base.
 * Les donnees chiffrees sont prefixees pour conserver la compatibilite
 * avec les anciennes lignes encore en clair.
 */
public final class SensitiveDataCipher {

    private static final String PREFIX = "ENC::";
    private static final String SECRET = ConfigLoader.get("STORAGE_AES_KEY", null);
    private static final SecretKey STORAGE_KEY = buildStorageKey();

    private SensitiveDataCipher() {}

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        if (isEncrypted(plainText)) {
            return plainText;
        }

        try {
            return PREFIX + AESEncryptor.encryptCompact(plainText, STORAGE_KEY);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de chiffrer la donnee sensible.", e);
        }
    }

    public static String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }
        if (!isEncrypted(storedValue)) {
            return storedValue;
        }

        try {
            return AESEncryptor.decryptCompact(storedValue.substring(PREFIX.length()), STORAGE_KEY);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de dechiffrer la donnee sensible.", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static SecretKey buildStorageKey() {
        if (SECRET == null || SECRET.isBlank()) {
            throw new IllegalStateException(
                    "La variable STORAGE_AES_KEY est requise pour chiffrer les donnees sensibles.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(SECRET.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de construire la cle de stockage AES.", e);
        }
    }
}
