package ma.ensate.test;

import ma.ensate.security.KeySerializer;
import ma.ensate.security.RSAKeyPairGenerator;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Utilitaire pour générer et sauvegarder les paires de clés RSA pour les administrateurs
 * 
 * Usage:
 * 1. Exécuter cette classe pour générer une nouvelle paire de clés
 * 2. La clé publique sera affichée pour être stockée dans la base de données
 * 3. La clé privée sera sauvegardée dans un fichier sécurisé
 * 
 * IMPORTANT: La clé privée doit être conservée en sécurité et ne doit jamais être partagée!
 */
public class AdminKeyGenerator {

    public static void main(String[] args) {
        try {
            System.out.println("=== Générateur de clés RSA pour Administrateur ===\n");

            // Générer la paire de clés
            System.out.println("Génération de la paire de clés RSA (2048 bits)...");
            KeyPair keyPair = RSAKeyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            System.out.println("✓ Paire de clés générée avec succès\n");

            // Sérialiser les clés
            String publicKeyBase64 = KeySerializer.serializePublicKey(publicKey);
            String privateKeyBase64 = KeySerializer.serializePrivateKey(privateKey);

            // Afficher la clé publique (à stocker dans la base de données)
            System.out.println("=== CLÉ PUBLIQUE (à stocker dans la base de données) ===");
            System.out.println(publicKeyBase64);
            System.out.println("=== FIN CLÉ PUBLIQUE ===\n");

            // Sauvegarder la clé privée dans un fichier
            String privateKeyPath = "admin_private_key.pem";
            savePrivateKeyToFile(privateKeyBase64, privateKeyPath);
            System.out.println("✓ Clé privée sauvegardée dans : " + new File(privateKeyPath).getAbsolutePath());
            System.out.println("⚠ IMPORTANT: Conservez ce fichier en sécurité et ne le partagez jamais!\n");

            // Instructions
            System.out.println("=== INSTRUCTIONS ===");
            System.out.println("1. Copiez la clé publique affichée ci-dessus");
            System.out.println("2. Exécutez la requête SQL suivante pour l'associer à un admin:");
            System.out.println("   UPDATE utilisateur SET public_key = '" + publicKeyBase64 + "' WHERE email = 'admin@example.com';");
            System.out.println("3. Conservez le fichier " + privateKeyPath + " en sécurité sur la machine de l'admin");
            System.out.println("4. Utilisez ce fichier lors de la connexion admin via l'interface challenge-response\n");

            // Optionnel: tester la signature
            System.out.println("=== TEST DE SIGNATURE ===");
            String testChallenge = "test_challenge_123";
            byte[] signature = ma.ensate.security.RSASigner.sign(testChallenge, privateKey);
            boolean valid = ma.ensate.security.RSAVerifier.verify(testChallenge, signature, publicKey);
            System.out.println("✓ Test de signature: " + (valid ? "SUCCÈS" : "ÉCHEC"));

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des clés: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sauvegarde la clé privée au format PEM
     */
    private static void savePrivateKeyToFile(String privateKeyBase64, String filePath) throws Exception {
        String pemContent = "-----BEGIN PRIVATE KEY-----\n";
        
        // Ajouter des sauts de ligne tous les 64 caractères (format PEM standard)
        for (int i = 0; i < privateKeyBase64.length(); i += 64) {
            int end = Math.min(i + 64, privateKeyBase64.length());
            pemContent += privateKeyBase64.substring(i, end) + "\n";
        }
        
        pemContent += "-----END PRIVATE KEY-----\n";
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(pemContent);
        }
    }

    /**
     * Sauvegarde la clé publique au format PEM
     */
    private static void savePublicKeyToFile(String publicKeyBase64, String filePath) throws Exception {
        String pemContent = "-----BEGIN PUBLIC KEY-----\n";
        
        for (int i = 0; i < publicKeyBase64.length(); i += 64) {
            int end = Math.min(i + 64, publicKeyBase64.length());
            pemContent += publicKeyBase64.substring(i, end) + "\n";
        }
        
        pemContent += "-----END PUBLIC KEY-----\n";
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(pemContent);
        }
    }
}
