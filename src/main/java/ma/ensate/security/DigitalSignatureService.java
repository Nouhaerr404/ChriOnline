package ma.ensate.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Service to handle digital signatures using SHA256withRSA.
 * Following the tutorial section 8: "Cas d'utilisation : Application Desktop de Signature Numérique"
 */
public class DigitalSignatureService {

    private static final String ALGORITHM = "SHA256withRSA";

    /**
     * Signs data using a private key.
     * 
     * @param data Data to sign
     * @param privateKey Private key to use for signing
     * @return The signature bytes
     * @throws Exception If signing fails
     */
    public byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verifies a signature using a public key.
     * 
     * @param data Original data
     * @param signatureBytes Signature bytes to verify
     * @param publicKey Public key to use for verification
     * @return true if valid, false otherwise
     * @throws Exception If verification fails
     */
    public boolean verifySignature(byte[] data, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
}
