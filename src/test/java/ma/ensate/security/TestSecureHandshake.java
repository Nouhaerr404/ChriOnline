package ma.ensate.security;

import ma.ensate.protocol.dto.HandshakeRequest;
import ma.ensate.protocol.dto.HandshakeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import javax.crypto.SecretKey;

public class TestSecureHandshake {

    private SecureHandshake server;
    private SecureHandshake client;

    @BeforeEach
    public void setUp() throws Exception {
        server = new SecureHandshake();  // Serveur avec clés RSA
        client = new SecureHandshake(true);  // Client
    }

    @Test
    public void testFullHandshakeFlow() throws Exception {
        // Phase 1: Client demande clé
        HandshakeRequest req1 = client.initiateHandshake();
        assertNotNull(req1);
        assertEquals("REQUEST_PUBLIC_KEY", req1.getPhase());
        System.out.println(" Phase 1: Client demande clé");

        // Phase 2: Serveur envoie clé
        HandshakeResponse resp = server.sendPublicKey(req1);
        assertNotNull(resp);
        assertEquals("PUBLIC_KEY", resp.getPhase());
        assertNotNull(resp.getPublicKeyBase64());
        System.out.println(" Phase 2: Serveur envoie clé");

        // Phase 3: Client chiffre AES
        HandshakeRequest req2 = client.sendEncryptedAESKey(resp, req1.getNonce());
        assertNotNull(req2);
        assertEquals("SEND_ENCRYPTED_AES", req2.getPhase());
        assertNotNull(req2.getEncryptedAESKey());
        System.out.println(" Phase 3: Client chiffre et envoie AES");

        // Phase 4: Serveur déchiffre AES
        HandshakeResponse okResp = server.receiveEncryptedAESKey(req2);
        assertEquals("HANDSHAKE_COMPLETE", okResp.getPhase());
        System.out.println(" Phase 4: Serveur déchiffre AES");

        // Vérification: les clés AES doivent être identiques
        SecretKey serverKey = server.getNegotiatedAESKey();
        SecretKey clientKey = client.getNegotiatedAESKey();

        assertArrayEquals(serverKey.getEncoded(), clientKey.getEncoded());
        System.out.println(" Les clés AES correspondent!");
    }

    @Test
    public void testReplayProtection() throws Exception {
        // Setup handshake
        HandshakeRequest req1 = client.initiateHandshake();
        HandshakeResponse resp = server.sendPublicKey(req1);

        // Deuxième fois avec le même nonce doit échouer
        assertThrows(SecurityException.class, () -> {
            server.sendPublicKey(req1);
        });
        System.out.println("Protection rejeu fonctionne");
    }
}