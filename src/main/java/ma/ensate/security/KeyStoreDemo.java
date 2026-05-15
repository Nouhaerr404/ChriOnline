package ma.ensate.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Demo class to verify KeyStore loading and Digital Signature functionality.
 */
public class KeyStoreDemo {

    public static void main(String[] args) {
        try {
            System.out.println("--- Demarrage de la demo KeyStore ---");

            // 1. Charger le Keystore
            String keystorePath = "monkeystore.p12";
            String password = "motdepasse";
            String alias = "monalias";

            System.out.println("Chargement du keystore: " + keystorePath);
            KeyStoreManager ksm = new KeyStoreManager(keystorePath, password);

            // 2. Recuperer les cles
            System.out.println("Recuperation de la cle privee pour l'alias: " + alias);
            PrivateKey privateKey = ksm.getPrivateKey(alias, password);
            
            System.out.println("Recuperation de la cle publique pour l'alias: " + alias);
            PublicKey publicKey = ksm.getPublicKey(alias);

            if (privateKey != null && publicKey != null) {
                System.out.println("Cles recuperees avec succes.");
            } else {
                System.err.println("Erreur: Cles non trouvees.");
                return;
            }

            // 3. Signer des donnees
            String message = "Ceci est un document confidentiel a signer.";
            System.out.println("\nMessage a signer: " + message);

            DigitalSignatureService dss = new DigitalSignatureService();
            byte[] signature = dss.signData(message.getBytes(), privateKey);
            
            System.out.println("Signature generee (Base64): " + Base64.getEncoder().encodeToString(signature));

            // 4. Verifier la signature
            boolean isValid = dss.verifySignature(message.getBytes(), signature, publicKey);
            System.out.println("Verification de la signature: " + (isValid ? "VALIDE" : "INVALIDE"));

            // 5. Test avec donnees modifiees
            String alteredMessage = "Ceci est un document modifie.";
            boolean isStillValid = dss.verifySignature(alteredMessage.getBytes(), signature, publicKey);
            System.out.println("Verification avec message modifie: " + (isStillValid ? "VALIDE (ATTENTION!)" : "INVALIDE (Correct)"));

            System.out.println("\n--- Fin de la demo ---");

        } catch (Exception e) {
            System.err.println("Erreur lors de la demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
