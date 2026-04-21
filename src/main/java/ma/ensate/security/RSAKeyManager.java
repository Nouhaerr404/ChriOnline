package ma.ensate.security;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Gestionnaire de clés RSA pour le protocole sécurisé
 * Responsable de la génération, sérialisation et stockage des clés RSA
 */
public class RSAKeyManager {

    private static final int KEY_SIZE = 2048;

    private KeyPair serverKeyPair;

    /**
     * Constructeur - génère une nouvelle paire de clés RSA pour le serveur
     */
    public RSAKeyManager() throws NoSuchAlgorithmException {
        this.serverKeyPair = generateKeyPair();
    }

    /**
     * Constructeur avec une paire de clés existante
     */
    public RSAKeyManager(KeyPair keyPair) {
        this.serverKeyPair = keyPair;
    }

    /**
     * Génère une nouvelle paire de clés RSA
     */
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_SIZE);
        return generator.generateKeyPair();
    }

    /**
     * Récupère la clé publique du serveur
     */
    public PublicKey getServerPublicKey() {
        return serverKeyPair.getPublic();
    }

    /**
     * Récupère la clé privée du serveur
     */
    public PrivateKey getServerPrivateKey() {
        return serverKeyPair.getPrivate();
    }

    /**
     * Sérialise la clé publique en Base64
     */
    public String serializePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Sérialise la clé privée en Base64
     */
    public String serializePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Désérialise une clé publique depuis Base64
     */
    public static PublicKey deserializePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Désérialise une clé privée depuis Base64
     */
    public static PrivateKey deserializePrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Sérialise la clé publique du serveur en Base64
     */
    public String getServerPublicKeyBase64() {
        return serializePublicKey(serverKeyPair.getPublic());
    }

    /**
     * Sérialise la clé privée du serveur en Base64
     */
    public String getServerPrivateKeyBase64() {
        return serializePrivateKey(serverKeyPair.getPrivate());
    }

    /**
     * Génère une paire de clés RSA statique
     */
    public static KeyPair generateStaticKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_SIZE);
        return generator.generateKeyPair();
    }

    /**
     * Génère une paire de clés RSA avec une taille personnalisée
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }
}
