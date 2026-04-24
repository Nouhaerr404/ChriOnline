package ma.ensate.protocol.dto;

import java.io.Serializable;
import java.util.UUID;

public class HandshakeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nonce;                   // UUID unique pour ce handshake
    private long timestamp;                 // Quand la requête a été créée
    private String phase;                   // "REQUEST_PUBLIC_KEY" ou "SEND_ENCRYPTED_AES"
    private byte[] encryptedAESKey;         // (optionnel) Clé AES chiffrée

    /**
     * Phase 1: Demander clé publique
     */
    public HandshakeRequest() {
        this.nonce = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.phase = "REQUEST_PUBLIC_KEY";
        this.encryptedAESKey = null;
    }

    /**
     * Phase 2: Envoyer clé AES chiffrée
     */
    public HandshakeRequest(String originalNonce, byte[] encryptedAESKey) {
        this.nonce = originalNonce; // On utilise le nonce original au lieu d'en créer un nouveau
        this.timestamp = System.currentTimeMillis();
        this.phase = "SEND_ENCRYPTED_AES";
        this.encryptedAESKey = encryptedAESKey;
    }


    // Getters
    public String getNonce() { return nonce; }
    public long getTimestamp() { return timestamp; }
    public String getPhase() { return phase; }
    public byte[] getEncryptedAESKey() { return encryptedAESKey; }

    // Validation
    public boolean isValid(long maxAgeMs) {
        return (System.currentTimeMillis() - timestamp) <= maxAgeMs;
    }
}