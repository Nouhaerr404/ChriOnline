package ma.ensate.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * Utility class to manage Java KeyStore operations.
 * Following the tutorial: "Utilisation de Java KeyStore (Keystore) pour la gestion des clés et certificats"
 */
public class KeyStoreManager {

    private KeyStore keyStore;

    /**
     * Loads a PKCS12 keystore from the classpath.
     * 
     * @param keystorePath Path to the keystore file in resources (e.g., "monkeystore.p12")
     * @param password Keystore password
     * @throws Exception If loading fails
     */
    public KeyStoreManager(String keystorePath, String password) throws Exception {
        this.keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(keystorePath)) {
            if (is == null) {
                throw new Exception("Keystore file not found in classpath: " + keystorePath);
            }
            keyStore.load(is, password.toCharArray());
        }
    }

    /**
     * Retrieves a private key from the keystore.
     * 
     * @param alias Key alias
     * @param keyPassword Key password
     * @return The private key
     * @throws Exception If retrieval fails
     */
    public PrivateKey getPrivateKey(String alias, String keyPassword) throws Exception {
        KeyStore.Entry entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        }
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }

    /**
     * Retrieves a public key from the keystore (via certificate).
     * 
     * @param alias Certificate alias
     * @return The public key
     * @throws Exception If retrieval fails
     */
    public PublicKey getPublicKey(String alias) throws Exception {
        Certificate cert = keyStore.getCertificate(alias);
        if (cert != null) {
            return cert.getPublicKey();
        }
        return null;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
}
