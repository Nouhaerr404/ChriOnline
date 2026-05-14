package ma.ensate.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal de communication sécurisée
 *
 * Chiffre/déchiffre Request et Response avec AES-256-GCM
 * Protège contre rejeu avec nonce + timestamp
 */
public class SecureChannel {

    private static final long MESSAGE_VALIDITY_MS = 60000;  // 1 minute

    private final SecretKey aesKey;
    private final DataOutput out;
    private final DataInput in;

    private final Map<String, Long> usedNonces;  // Protection rejeu

    /**
     * Créer canal sécurisé avec clé AES (établie par handshake)
     */
    public SecureChannel(SecretKey aesKey, InputStream in, OutputStream out) {
        this.aesKey = aesKey;
        // Si les streams sont déjà DataInput/DataOutput (comme ObjectStream), on les utilise directement
        this.in = (in instanceof DataInput) ? (DataInput)in : new DataInputStream(in);
        this.out = (out instanceof DataOutput) ? (DataOutput)out : new DataOutputStream(out);
        this.usedNonces = new ConcurrentHashMap<>();

        System.out.println("[CANAL] Sécurisé établi");
    }

    public synchronized void writeSecureRequest(Request request) throws Exception {
        String data = serializeToString(request);
        byte[] encrypted = encryptMessage(data);
        out.writeInt(encrypted.length);
        out.write(encrypted);
        if (out instanceof Flushable) ((Flushable)out).flush();
    }

    public synchronized void writeSecureResponse(Response response) throws Exception {
        String data = serializeToString(response);
        byte[] encrypted = encryptMessage(data);
        out.writeInt(encrypted.length);
        out.write(encrypted);
        if (out instanceof Flushable) ((Flushable)out).flush();
    }

    public synchronized Request readSecureRequest() throws Exception {
        byte[] encrypted = readEncryptedMessage();
        String data = decryptMessage(encrypted);
        return (Request) deserializeFromString(data);
    }

    public synchronized Response readSecureResponse() throws Exception {
        byte[] encrypted = readEncryptedMessage();
        String data = decryptMessage(encrypted);
        return (Response) deserializeFromString(data);
    }

    private byte[] encryptMessage(String plaintext) throws Exception {
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        String prefixed = nonce + "|" + timestamp + "|" + plaintext;

        byte[] iv = AESKeyGenerator.generateIV();
        byte[] ciphertext = AESEncryptor.encrypt(prefixed.getBytes(StandardCharsets.UTF_8), aesKey, iv);

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    private String decryptMessage(byte[] encrypted) throws Exception {
        if (encrypted.length < 12) throw new SecurityException("Message trop court");
        
        byte[] iv = new byte[12];
        System.arraycopy(encrypted, 0, iv, 0, 12);
        byte[] ciphertext = new byte[encrypted.length - 12];
        System.arraycopy(encrypted, 12, ciphertext, 0, ciphertext.length);

        byte[] decrypted = AESEncryptor.decrypt(ciphertext, aesKey, iv);
        String plaintext = new String(decrypted, StandardCharsets.UTF_8);

        String[] parts = plaintext.split("\\|", 3);
        if (parts.length < 3) throw new SecurityException("Format invalide");

        String nonce = parts[0];
        long timestamp = Long.parseLong(parts[1]);
        String data = parts[2];

        if (System.currentTimeMillis() - timestamp > MESSAGE_VALIDITY_MS)
            throw new SecurityException("Message expiré");
        if (usedNonces.containsKey(nonce))
            throw new SecurityException("Rejeu détecté");
        
        usedNonces.put(nonce, System.currentTimeMillis());
        return data;
    }

    private byte[] readEncryptedMessage() throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 10 * 1024 * 1024) throw new IOException("Taille invalide: " + length);
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    private String serializeToString(Object obj) throws Exception {
        if (obj == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private Object deserializeFromString(String str) throws Exception {
        if (str == null || str.isEmpty()) return null;
        byte[] bytes = Base64.getDecoder().decode(str);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
}
