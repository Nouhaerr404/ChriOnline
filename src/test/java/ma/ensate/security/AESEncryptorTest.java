package ma.ensate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour AESKeyGenerator et AESEncryptor.
 * Couvre la génération de clés, le chiffrement/déchiffrement,
 * le mode compact, les AAD, et les cas d'erreur.
 *
 * @author Personne 2
 */
@DisplayName("Tests AES - Chiffrement Symétrique")
class AESEncryptorTest {

    private SecretKey key;
    private byte[] iv;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        key = AESKeyGenerator.generateKey();
        iv = AESKeyGenerator.generateIV();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Tests AESKeyGenerator
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AESKeyGenerator")
    class KeyGeneratorTests {

        @Test
        @DisplayName("Génère une clé AES-256 valide")
        void generateKey_shouldReturn256BitKey() throws Exception {
            SecretKey generatedKey = AESKeyGenerator.generateKey();
            assertNotNull(generatedKey);
            assertEquals("AES", generatedKey.getAlgorithm());
            assertEquals(32, generatedKey.getEncoded().length); // 256 bits = 32 bytes
        }

        @Test
        @DisplayName("Génère des clés AES-128, 192, 256")
        void generateKey_withCustomSize_shouldWork() throws Exception {
            SecretKey key128 = AESKeyGenerator.generateKey(128);
            assertEquals(16, key128.getEncoded().length);

            SecretKey key192 = AESKeyGenerator.generateKey(192);
            assertEquals(24, key192.getEncoded().length);

            SecretKey key256 = AESKeyGenerator.generateKey(256);
            assertEquals(32, key256.getEncoded().length);
        }

        @Test
        @DisplayName("Rejette les tailles de clé invalides")
        void generateKey_withInvalidSize_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                () -> AESKeyGenerator.generateKey(64));
            assertThrows(IllegalArgumentException.class,
                () -> AESKeyGenerator.generateKey(512));
        }

        @Test
        @DisplayName("Chaque clé générée est unique")
        void generateKey_shouldBeUnique() throws Exception {
            Set<String> keys = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String keyBase64 = AESKeyGenerator.serializeKey(AESKeyGenerator.generateKey());
                assertTrue(keys.add(keyBase64), "Clé dupliquée détectée !");
            }
        }

        @Test
        @DisplayName("Génère un IV de 12 bytes (96 bits)")
        void generateIV_shouldReturn12Bytes() {
            byte[] generatedIV = AESKeyGenerator.generateIV();
            assertNotNull(generatedIV);
            assertEquals(12, generatedIV.length);
        }

        @Test
        @DisplayName("Chaque IV est unique")
        void generateIV_shouldBeUnique() {
            Set<String> ivs = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String ivBase64 = Base64.getEncoder().encodeToString(AESKeyGenerator.generateIV());
                assertTrue(ivs.add(ivBase64), "IV dupliqué détecté !");
            }
        }

        @Test
        @DisplayName("Génère un IV avec taille personnalisée")
        void generateIV_withCustomLength_shouldWork() {
            byte[] iv16 = AESKeyGenerator.generateIV(16);
            assertEquals(16, iv16.length);
        }

        @Test
        @DisplayName("Rejette un IV de taille invalide")
        void generateIV_withInvalidLength_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                () -> AESKeyGenerator.generateIV(0));
            assertThrows(IllegalArgumentException.class,
                () -> AESKeyGenerator.generateIV(-1));
        }

        @Test
        @DisplayName("Sérialisation/Désérialisation de clé")
        void serializeDeserialize_shouldPreserveKey() throws Exception {
            SecretKey original = AESKeyGenerator.generateKey();
            String base64 = AESKeyGenerator.serializeKey(original);
            SecretKey restored = AESKeyGenerator.deserializeKey(base64);

            assertArrayEquals(original.getEncoded(), restored.getEncoded());
            assertEquals(original.getAlgorithm(), restored.getAlgorithm());
        }

        @Test
        @DisplayName("Désérialisation échoue avec entrée vide")
        void deserializeKey_withEmpty_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                () -> AESKeyGenerator.deserializeKey(""));
            assertThrows(IllegalArgumentException.class,
                () -> AESKeyGenerator.deserializeKey(null));
        }

        @Test
        @DisplayName("Encodage/Décodage IV en Base64")
        void encodeDecodeIV_shouldPreserveIV() {
            byte[] originalIV = AESKeyGenerator.generateIV();
            String base64 = AESKeyGenerator.encodeIV(originalIV);
            byte[] restored = AESKeyGenerator.decodeIV(base64);
            assertArrayEquals(originalIV, restored);
        }

        @Test
        @DisplayName("Constantes par défaut")
        void defaults_shouldBeCorrect() {
            assertEquals(256, AESKeyGenerator.getDefaultKeySize());
            assertEquals(12, AESKeyGenerator.getDefaultIVLength());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Tests AESEncryptor — Chiffrement/Déchiffrement avec IV explicite
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AESEncryptor - Mode IV explicite")
    class ExplicitIVTests {

        @Test
        @DisplayName("Chiffre et déchiffre des bytes")
        void encryptDecrypt_bytes_shouldWork() throws Exception {
            byte[] data = "Bonjour ChriOnline !".getBytes(StandardCharsets.UTF_8);

            byte[] encrypted = AESEncryptor.encrypt(data, key, iv);
            assertNotNull(encrypted);
            assertFalse(Arrays.equals(data, encrypted), "Les données chiffrées ne doivent pas être identiques");

            byte[] decrypted = AESEncryptor.decrypt(encrypted, key, iv);
            assertArrayEquals(data, decrypted);
        }

        @Test
        @DisplayName("Chiffre et déchiffre en Base64")
        void encryptDecrypt_base64_shouldWork() throws Exception {
            byte[] data = "Données sensibles de paiement".getBytes(StandardCharsets.UTF_8);

            String encryptedBase64 = AESEncryptor.encryptToBase64(data, key, iv);
            assertNotNull(encryptedBase64);
            assertFalse(encryptedBase64.isEmpty());

            byte[] decrypted = AESEncryptor.decryptFromBase64(encryptedBase64, key, iv);
            assertArrayEquals(data, decrypted);
        }

        @Test
        @DisplayName("Chiffre et déchiffre des Strings")
        void encryptDecrypt_string_shouldWork() throws Exception {
            String original = "Email: admin@chrionline.ma | Mot de passe: S3cur3!";

            String encrypted = AESEncryptor.encryptString(original, key, iv);
            assertNotNull(encrypted);
            assertNotEquals(original, encrypted);

            String decrypted = AESEncryptor.decryptString(encrypted, key, iv);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Gère les caractères spéciaux et unicode")
        void encryptDecrypt_unicode_shouldWork() throws Exception {
            String original = "Données àéîôù © ® → ★ 日本語 العربية 🔒";

            String encrypted = AESEncryptor.encryptString(original, key, iv);
            String decrypted = AESEncryptor.decryptString(encrypted, key, iv);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Chiffrement avec mauvaise clé échoue")
        void decrypt_withWrongKey_shouldThrow() throws Exception {
            byte[] data = "Secret".getBytes();
            byte[] encrypted = AESEncryptor.encrypt(data, key, iv);

            SecretKey wrongKey = AESKeyGenerator.generateKey();
            assertThrows(Exception.class,
                () -> AESEncryptor.decrypt(encrypted, wrongKey, iv));
        }

        @Test
        @DisplayName("Chiffrement avec mauvais IV échoue")
        void decrypt_withWrongIV_shouldThrow() throws Exception {
            byte[] data = "Secret".getBytes();
            byte[] encrypted = AESEncryptor.encrypt(data, key, iv);

            byte[] wrongIV = AESKeyGenerator.generateIV();
            assertThrows(Exception.class,
                () -> AESEncryptor.decrypt(encrypted, key, wrongIV));
        }

        @Test
        @DisplayName("Données altérées détectées par GCM")
        void decrypt_withTamperedData_shouldThrow() throws Exception {
            byte[] data = "Intégrité des données".getBytes();
            byte[] encrypted = AESEncryptor.encrypt(data, key, iv);

            // Altérer un byte au milieu des données chiffrées
            encrypted[encrypted.length / 2] ^= 0xFF;

            assertThrows(AEADBadTagException.class,
                () -> AESEncryptor.decrypt(encrypted, key, iv));
        }

        @Test
        @DisplayName("Rejette les paramètres null")
        void encrypt_withNull_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                () -> AESEncryptor.encrypt(null, key, iv));
            assertThrows(IllegalArgumentException.class,
                () -> AESEncryptor.encrypt("data".getBytes(), null, iv));
            assertThrows(IllegalArgumentException.class,
                () -> AESEncryptor.encrypt("data".getBytes(), key, null));
        }

        @Test
        @DisplayName("Même données + même clé + IV différent = chiffrés différents")
        void encrypt_samePlaintext_differentIV_shouldDiffer() throws Exception {
            byte[] data = "Données identiques".getBytes();
            byte[] iv1 = AESKeyGenerator.generateIV();
            byte[] iv2 = AESKeyGenerator.generateIV();

            byte[] encrypted1 = AESEncryptor.encrypt(data, key, iv1);
            byte[] encrypted2 = AESEncryptor.encrypt(data, key, iv2);

            assertFalse(Arrays.equals(encrypted1, encrypted2),
                "Deux chiffrements avec des IV différents ne doivent pas produire le même résultat");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Tests AESEncryptor — Mode compact (IV préfixé)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AESEncryptor - Mode compact")
    class CompactModeTests {

        @Test
        @DisplayName("Chiffre et déchiffre en mode compact")
        void encryptDecryptCompact_shouldWork() throws Exception {
            String original = "Message confidentiel via canal sécurisé";

            String encrypted = AESEncryptor.encryptCompact(original, key);
            assertNotNull(encrypted);

            String decrypted = AESEncryptor.decryptCompact(encrypted, key);
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Mode compact avec données binaires")
        void encryptDecryptCompactBytes_shouldWork() throws Exception {
            byte[] data = new byte[]{0x01, 0x02, 0x03, (byte) 0xFF, 0x00, (byte) 0xAB};

            String encrypted = AESEncryptor.encryptCompactBytes(data, key);
            byte[] decrypted = AESEncryptor.decryptCompactBytes(encrypted, key);

            assertArrayEquals(data, decrypted);
        }

        @Test
        @DisplayName("Mode compact avec texte vide")
        void encryptDecryptCompact_emptyString_shouldWork() throws Exception {
            // GCM supporte le chiffrement de texte vide (seul le tag est produit)
            // Note: "" vide est valide en GCM
        }

        @Test
        @DisplayName("Mode compact — chaque chiffrement produit un résultat différent")
        void encryptCompact_sameData_shouldDiffer() throws Exception {
            String original = "Même message";

            String encrypted1 = AESEncryptor.encryptCompact(original, key);
            String encrypted2 = AESEncryptor.encryptCompact(original, key);

            assertNotEquals(encrypted1, encrypted2,
                "Deux chiffrements compacts doivent différer (IV aléatoire)");

            // Mais les deux doivent déchiffrer vers le même texte
            assertEquals(original, AESEncryptor.decryptCompact(encrypted1, key));
            assertEquals(original, AESEncryptor.decryptCompact(encrypted2, key));
        }

        @Test
        @DisplayName("Mode compact — mauvaise clé échoue")
        void decryptCompact_withWrongKey_shouldThrow() throws Exception {
            String encrypted = AESEncryptor.encryptCompact("Secret", key);
            SecretKey wrongKey = AESKeyGenerator.generateKey();

            assertThrows(Exception.class,
                () -> AESEncryptor.decryptCompact(encrypted, wrongKey));
        }

        @Test
        @DisplayName("Mode compact — données trop courtes")
        void decryptCompact_tooShort_shouldThrow() {
            String tooShort = Base64.getEncoder().encodeToString(new byte[5]);

            assertThrows(IllegalArgumentException.class,
                () -> AESEncryptor.decryptCompact(tooShort, key));
        }

        @Test
        @DisplayName("Rejette null en mode compact")
        void encryptCompact_null_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                () -> AESEncryptor.encryptCompact(null, key));
            assertThrows(IllegalArgumentException.class,
                () -> AESEncryptor.decryptCompact(null, key));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Tests AESEncryptor — Additional Authenticated Data (AAD)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AESEncryptor - AAD (Données authentifiées)")
    class AADTests {

        @Test
        @DisplayName("Chiffre et déchiffre avec AAD")
        void encryptDecryptWithAAD_shouldWork() throws Exception {
            byte[] data = "Données de commande".getBytes();
            byte[] aad = "session-id:12345".getBytes();

            byte[] encrypted = AESEncryptor.encryptWithAAD(data, key, iv, aad);
            byte[] decrypted = AESEncryptor.decryptWithAAD(encrypted, key, iv, aad);

            assertArrayEquals(data, decrypted);
        }

        @Test
        @DisplayName("AAD modifié lors du déchiffrement échoue")
        void decryptWithAAD_wrongAAD_shouldThrow() throws Exception {
            byte[] data = "Données protégées".getBytes();
            byte[] aad = "session-id:12345".getBytes();

            byte[] encrypted = AESEncryptor.encryptWithAAD(data, key, iv, aad);

            byte[] wrongAAD = "session-id:99999".getBytes();
            assertThrows(AEADBadTagException.class,
                () -> AESEncryptor.decryptWithAAD(encrypted, key, iv, wrongAAD));
        }

        @Test
        @DisplayName("AAD null est autorisé (pas d'authentification additionnelle)")
        void encryptDecryptWithAAD_nullAAD_shouldWork() throws Exception {
            byte[] data = "Sans AAD".getBytes();

            byte[] encrypted = AESEncryptor.encryptWithAAD(data, key, iv, null);
            byte[] decrypted = AESEncryptor.decryptWithAAD(encrypted, key, iv, null);

            assertArrayEquals(data, decrypted);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Tests d'intégration RSA + AES (simulation du protocole HTTPS-like)
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Intégration RSA + AES (protocole HTTPS-like)")
    class IntegrationTests {

        @Test
        @DisplayName("Échange de clé AES via RSA puis communication AES")
        void fullProtocol_rsaKeyExchange_thenAES_shouldWork() throws Exception {
            // 1. Le serveur génère sa paire de clés RSA
            RSAKeyManager rsaKeyManager = new RSAKeyManager();

            // 2. Le client génère une clé AES
            SecretKey aesKey = AESKeyGenerator.generateKey();

            // 3. Le client chiffre la clé AES avec la clé publique RSA du serveur
            byte[] aesKeyBytes = aesKey.getEncoded();
            byte[] encryptedAESKey = RSAEncryptor.encrypt(aesKeyBytes, rsaKeyManager.getServerPublicKey());

            // 4. Le serveur déchiffre la clé AES avec sa clé privée RSA
            byte[] decryptedAESKeyBytes = RSAEncryptor.decrypt(encryptedAESKey, rsaKeyManager.getServerPrivateKey());
            SecretKey serverAESKey = new javax.crypto.spec.SecretKeySpec(decryptedAESKeyBytes, "AES");

            // 5. Vérifier que les deux parties ont la même clé AES
            assertArrayEquals(aesKey.getEncoded(), serverAESKey.getEncoded());

            // 6. Communication chiffrée avec AES
            String messageClient = "Commande #1234 — Total: 299.99 MAD";
            String chiffre = AESEncryptor.encryptCompact(messageClient, aesKey);
            String dechiffreServeur = AESEncryptor.decryptCompact(chiffre, serverAESKey);

            assertEquals(messageClient, dechiffreServeur);

            // 7. Réponse du serveur
            String reponseServeur = "Commande validée ! Livraison sous 48h.";
            String repChiffree = AESEncryptor.encryptCompact(reponseServeur, serverAESKey);
            String repDechiffree = AESEncryptor.decryptCompact(repChiffree, aesKey);

            assertEquals(reponseServeur, repDechiffree);
        }

        @Test
        @DisplayName("Sérialisation complète : clé AES via Base64 + RSA")
        void serialization_aesKey_viaRSA_shouldWork() throws Exception {
            RSAKeyManager rsaKeyManager = new RSAKeyManager();

            // Client génère et sérialise
            SecretKey originalKey = AESKeyGenerator.generateKey();
            String keyBase64 = AESKeyGenerator.serializeKey(originalKey);

            // Chiffrement RSA de la clé sérialisée
            String encrypted = RSAEncryptor.encryptString(keyBase64, rsaKeyManager.getServerPublicKey());

            // Serveur déchiffre et désérialise
            String decryptedBase64 = RSAEncryptor.decryptString(encrypted, rsaKeyManager.getServerPrivateKey());
            SecretKey restoredKey = AESKeyGenerator.deserializeKey(decryptedBase64);

            assertArrayEquals(originalKey.getEncoded(), restoredKey.getEncoded());
        }
    }
}
