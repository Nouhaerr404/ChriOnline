package ma.ensate.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour RSAEncryptor
 * Valide le chiffrement et déchiffrement RSA
 */
public class RSAEncryptorTest {

    private static KeyPair keyPair;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    @BeforeAll
    static void setUp() throws NoSuchAlgorithmException {
        keyPair = RSAKeyManager.generateStaticKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    @Test
    @DisplayName("Test chiffrement/déchiffrement de bytes")
    public void testEncryptDecryptBytes() throws Exception {
        String originalMessage = "Message secret pour le test";
        byte[] originalBytes = originalMessage.getBytes();

        // Chiffrer
        byte[] encrypted = RSAEncryptor.encrypt(originalBytes, publicKey);
        assertNotNull(encrypted);
        assertNotEquals(originalBytes, encrypted);

        // Déchiffrer
        byte[] decrypted = RSAEncryptor.decrypt(encrypted, privateKey);
        assertNotNull(decrypted);
        assertArrayEquals(originalBytes, decrypted);
    }

    @Test
    @DisplayName("Test chiffrement/déchiffrement en Base64")
    public void testEncryptDecryptBase64() throws Exception {
        String originalMessage = "Message secret pour le test Base64";

        // Chiffrer
        String encryptedBase64 = RSAEncryptor.encryptString(originalMessage, publicKey);
        assertNotNull(encryptedBase64);
        assertNotEquals(originalMessage, encryptedBase64);

        // Déchiffrer
        String decrypted = RSAEncryptor.decryptString(encryptedBase64, privateKey);
        assertNotNull(decrypted);
        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Test taille maximale des données")
    public void testMaxDataSize() {
        int maxSize = RSAEncryptor.getMaxDataSize(2048);
        assertEquals(245, maxSize); // (2048/8) - 11 = 245
    }

    @Test
    @DisplayName("Test validation taille des données - données valides")
    public void testValidateDataSizeValid() {
        byte[] smallData = "petit message".getBytes();
        assertDoesNotThrow(() -> RSAEncryptor.validateDataSize(smallData, 2048));
    }

    @Test
    @DisplayName("Test validation taille des données - données trop grandes")
    public void testValidateDataSizeInvalid() {
        byte[] largeData = new byte[300]; // Plus grand que 245
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> RSAEncryptor.validateDataSize(largeData, 2048)
        );
        assertTrue(exception.getMessage().contains("trop grandes"));
    }

    @Test
    @DisplayName("Test canEncrypt - données valides")
    public void testCanEncryptValid() {
        byte[] smallData = "petit message".getBytes();
        assertTrue(RSAEncryptor.canEncrypt(smallData, 2048));
    }

    @Test
    @DisplayName("Test canEncrypt - données trop grandes")
    public void testCanEncryptInvalid() {
        byte[] largeData = new byte[300);
        assertFalse(RSAEncryptor.canEncrypt(largeData, 2048));
    }

    @Test
    @DisplayName("Test chiffrement avec validation")
    public void testEncryptWithValidation() throws Exception {
        String originalMessage = "Message validé";
        byte[] data = originalMessage.getBytes();

        String encryptedBase64 = RSAEncryptor.encryptWithValidation(data, publicKey);
        assertNotNull(encryptedBase64);

        String decrypted = RSAEncryptor.decryptString(encryptedBase64, privateKey);
        assertEquals(originalMessage, decrypted);
    }

    @Test
    @DisplayName("Test déchiffrement avec mauvaise clé privée")
    public void testDecryptWithWrongKey() throws Exception {
        String originalMessage = "Message secret";
        byte[] data = originalMessage.getBytes();

        String encryptedBase64 = RSAEncryptor.encryptString(originalMessage, publicKey);

        // Générer une autre paire de clés
        KeyPair wrongKeyPair = RSAKeyManager.generateStaticKeyPair();
        PrivateKey wrongPrivateKey = wrongKeyPair.getPrivate();

        // Tenter de déchiffrer avec la mauvaise clé
        assertThrows(Exception.class, () -> {
            RSAEncryptor.decryptString(encryptedBase64, wrongPrivateKey);
        });
    }

    @Test
    @DisplayName("Test chiffrement de données vides")
    public void testEncryptEmptyData() throws Exception {
        byte[] emptyData = new byte[0];

        byte[] encrypted = RSAEncryptor.encrypt(emptyData, publicKey);
        assertNotNull(encrypted);

        byte[] decrypted = RSAEncryptor.decrypt(encrypted, privateKey);
        assertArrayEquals(emptyData, decrypted);
    }

    @Test
    @DisplayName("Test sérialisation/désérialisation de clés")
    public void testKeySerialization() throws Exception {
        String publicKeyBase64 = RSAKeyManager.serializePublicKey(publicKey);
        String privateKeyBase64 = RSAKeyManager.serializePrivateKey(privateKey);

        assertNotNull(publicKeyBase64);
        assertNotNull(privateKeyBase64);

        PublicKey deserializedPublicKey = RSAKeyManager.deserializePublicKey(publicKeyBase64);
        PrivateKey deserializedPrivateKey = RSAKeyManager.deserializePrivateKey(privateKeyBase64);

        assertNotNull(deserializedPublicKey);
        assertNotNull(deserializedPrivateKey);

        // Tester que les clés désérialisées fonctionnent
        String message = "Test sérialisation";
        String encrypted = RSAEncryptor.encryptString(message, deserializedPublicKey);
        String decrypted = RSAEncryptor.decryptString(encrypted, deserializedPrivateKey);
        assertEquals(message, decrypted);
    }

    @Test
    @DisplayName("Test génération de paires de clés différentes")
    public void testDifferentKeyPairs() throws NoSuchAlgorithmException {
        KeyPair keyPair1 = RSAKeyManager.generateStaticKeyPair();
        KeyPair keyPair2 = RSAKeyManager.generateStaticKeyPair();

        assertNotEquals(
            keyPair1.getPublic().getEncoded(),
            keyPair2.getPublic().getEncoded()
        );
        assertNotEquals(
            keyPair1.getPrivate().getEncoded(),
            keyPair2.getPrivate().getEncoded()
        );
    }
}
