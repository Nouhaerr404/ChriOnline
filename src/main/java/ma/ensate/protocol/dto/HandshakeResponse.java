package ma.ensate.protocol.dto;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Réponse handshake serveur -> client
 */
public class HandshakeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nonce;                   // Nonce original du client
    private long timestamp;                 // Timestamp serveur
    private String publicKeyBase64;         // Clé publique RSA encodée Base64
    private String phase;                   // "PUBLIC_KEY" ou "HANDSHAKE_COMPLETE"

    /**
     * Réponse avec clé publique RSA
     */
    public HandshakeResponse(String clientNonce, PublicKey serverPublicKey) {
        this.nonce = clientNonce;
        this.timestamp = System.currentTimeMillis();
        this.phase = "PUBLIC_KEY";

        // Encoder clé publique en Base64
        byte[] encodedKey = serverPublicKey.getEncoded();
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(encodedKey);
    }

    /**
     * Réponse de confirmation handshake OK
     */
    public HandshakeResponse(String clientNonce, String phase) {
        this.nonce = clientNonce;
        this.timestamp = System.currentTimeMillis();
        this.phase = phase; // "HANDSHAKE_COMPLETE"
        this.publicKeyBase64 = null;
    }

    // Getters
    public String getNonce() { return nonce; }
    public long getTimestamp() { return timestamp; }
    public String getPublicKeyBase64() { return publicKeyBase64; }
    public String getPhase() { return phase; }

    // Validation
    public boolean isValid(long maxAgeMs) {
        return (System.currentTimeMillis() - timestamp) <= maxAgeMs;
    }
}