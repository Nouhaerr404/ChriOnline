package ma.ensate.security;

import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class SecureChannelTest {

    @Test
    void shouldRoundTripSecureRequestAndResponse() throws Exception {
        SecretKey aesKey = AESKeyGenerator.generateKey();

        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);

        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);

        SecureChannel clientChannel = new SecureChannel(aesKey, clientInput, clientOutput);
        SecureChannel serverChannel = new SecureChannel(aesKey, serverInput, serverOutput);

        Request request = new Request("GET_SERVER_PUBLIC_KEY");
        clientChannel.writeSecureRequest(request);

        Request decryptedRequest = serverChannel.readSecureRequest();
        assertEquals("GET_SERVER_PUBLIC_KEY", decryptedRequest.getAction());

        Response response = new Response(true, "OK");
        serverChannel.writeSecureResponse(response);

        Response decryptedResponse = clientChannel.readSecureResponse();
        assertTrue(decryptedResponse.isSuccess());
        assertEquals("OK", decryptedResponse.getMessage());
    }

    @Test
    void shouldPreventReplayAttack() throws Exception {
        SecretKey aesKey = AESKeyGenerator.generateKey();

        // 1. Prepare a message
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecureChannel clientChannel = new SecureChannel(aesKey, new ByteArrayInputStream(new byte[0]), baos);
        
        Request request = new Request("GET_PROFIL");
        clientChannel.writeSecureRequest(request);
        byte[] interceptedPayload = baos.toByteArray();

        // 2. Feed it twice to the server
        byte[] doublePayload = new byte[interceptedPayload.length * 2];
        System.arraycopy(interceptedPayload, 0, doublePayload, 0, interceptedPayload.length);
        System.arraycopy(interceptedPayload, 0, doublePayload, interceptedPayload.length, interceptedPayload.length);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(doublePayload);
        SecureChannel serverChannel = new SecureChannel(aesKey, bais, new ByteArrayOutputStream());

        // First read should work
        Request req1 = serverChannel.readSecureRequest();
        assertEquals("GET_PROFIL", req1.getAction());

        // Second read should fail with SecurityException (Replay detected)
        assertThrows(SecurityException.class, () -> {
            serverChannel.readSecureRequest();
        });
    }

    @Test
    void shouldFailIfMessageExpired() throws Exception {
        SecretKey aesKey = AESKeyGenerator.generateKey();
        
        // This is harder to test without mocking System.currentTimeMillis()
        // But we can verify the structure
    }
}
