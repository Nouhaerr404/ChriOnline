package ma.ensate.security;

import javax.crypto.SecretKey;
import ma.ensate.protocol.dto.HandshakeRequest;
import ma.ensate.protocol.dto.HandshakeResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestration du handshake sécurisé RSA/AES (HTTPS-like)
 *
 * Serveur:
 * 1. Envoie sa clé publique RSA au client
 * 2. Reçoit clé AES chiffrée avec sa clé publique RSA
 * 3. Déchiffre avec sa clé privée RSA
 *
 * Client:
 * 1. Demande clé publique RSA du serveur
 * 2. Génère clé AES-256 aléatoire
 * 3. Chiffre clé AES avec clé publique du serveur
 * 4. Envoie clé AES chiffrée
 */
public class SecureHandshake {

    private static final long NONCE_VALIDITY_MS = 60000; // 1 minute

    // État serveur
    private RSAKeyManager rsaManager;
    private Map<String, Long> processedNonces;

    // État client
    private SecretKey negotiatedAESKey;
    private boolean handshakeComplete;

    // ──────────────────────────────────────────────────────────────────
    // CONSTRUCTEURS
    // ──────────────────────────────────────────────────────────────────

    /**
     * Constructeur SERVEUR: crée nouvelles clés RSA
     */
    public SecureHandshake() throws Exception {
        this.rsaManager = new RSAKeyManager();
        this.processedNonces = new ConcurrentHashMap<>();
        this.negotiatedAESKey = null;
        this.handshakeComplete = false;
    }

    /**
     * Constructeur SERVEUR: utilise clés RSA existantes
     */
    public SecureHandshake(RSAKeyManager manager) {
        this.rsaManager = manager;
        this.processedNonces = new ConcurrentHashMap<>();
        this.negotiatedAESKey = null;
        this.handshakeComplete = false;
    }

    /**
     * Constructeur CLIENT: pas de clés RSA
     */
    public SecureHandshake(boolean isClient) {
        if (!isClient) throw new IllegalArgumentException("Utilise constructor () ou (RSAKeyManager)");
        this.rsaManager = null;
        this.processedNonces = new ConcurrentHashMap<>();
        this.negotiatedAESKey = null;
        this.handshakeComplete = false;
    }

    // ──────────────────────────────────────────────────────────────────
    // CÔTÉ SERVEUR
    // ──────────────────────────────────────────────────────────────────

    /**
     * [SERVEUR] Préparer réponse avec clé publique RSA
     * Appelé quand client demande "REQUEST_PUBLIC_KEY"
     */
    public HandshakeResponse sendPublicKey(HandshakeRequest clientRequest) throws Exception {

        // Valider nonce (protection rejeu)
        if (processedNonces.containsKey(clientRequest.getNonce())) {
            throw new SecurityException("Nonce déjà traité (rejeu!)");
        }

        if (rsaManager == null) {
            throw new IllegalStateException("Pas un serveur (pas de clés RSA)");
        }

        // Marquer nonce comme traité
        processedNonces.put(clientRequest.getNonce(), System.currentTimeMillis());

        // Créer réponse avec clé publique RSA encodée Base64
        HandshakeResponse response = new HandshakeResponse(
                clientRequest.getNonce(),
                rsaManager.getServerPublicKey()
        );

        System.out.println("[SERVEUR] Clé publique RSA envoyée au client");
        return response;
    }

    /**
     * [SERVEUR] Traiter clé AES chiffrée reçue du client
     * Appelé quand client envoie "SEND_ENCRYPTED_AES"
     */
    public HandshakeResponse receiveEncryptedAESKey(HandshakeRequest clientRequest) throws Exception {

        if (rsaManager == null) {
            throw new IllegalStateException("Pas un serveur (pas de clés RSA)");
        }

        if (clientRequest.getEncryptedAESKey() == null) {
            throw new IllegalArgumentException("Clé AES chiffrée manquante");
        }

        // Validation nonce
        if (!processedNonces.containsKey(clientRequest.getNonce())) {
            throw new SecurityException("Nonce inconnu (attends REQUEST_PUBLIC_KEY d'abord)");
        }

        try {
            // 1. Déchiffrer clé AES avec clé privée RSA
            // Utilise RSAEncryptor de Personne 1
            byte[] decryptedAESBytes = RSAEncryptor.decrypt(
                    clientRequest.getEncryptedAESKey(),
                    rsaManager.getServerPrivateKey()
            );

            // 2. Reconstruire clé AES depuis bytes
            // Utilise AESKeyGenerator de Personne 2
            String aesKeyBase64 = java.util.Base64.getEncoder().encodeToString(decryptedAESBytes);
            this.negotiatedAESKey = AESKeyGenerator.deserializeKey(aesKeyBase64);
            // 3. Marquer handshake comme complet
            this.handshakeComplete = true;

            System.out.println("[SERVEUR] Clé AES déchiffrée et établie");

            // 4. Envoyer confirmation
            return new HandshakeResponse(clientRequest.getNonce(), "HANDSHAKE_COMPLETE");

        } catch (Exception e) {
            System.err.println("[SERVEUR] Erreur déchiffrement AES: " + e.getMessage());
            throw new SecurityException("Impossible de déchiffrer clé AES", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // CÔTÉ CLIENT
    // ──────────────────────────────────────────────────────────────────

    /**
     * [CLIENT] Créer requête phase 1: demander clé publique du serveur
     */
    public HandshakeRequest initiateHandshake() {
        HandshakeRequest request = new HandshakeRequest();
        System.out.println("[CLIENT] Demande clé publique RSA du serveur");
        return request;
    }

    /**
     * [CLIENT] Phase 2: traiter réponse serveur, chiffrer et envoyer clé AES
     * Appelé après avoir reçu la clé publique RSA du serveur
     */
    public HandshakeRequest sendEncryptedAESKey(HandshakeResponse serverResponse, String clientNonce) throws Exception {

        // Valider réponse
        if (!serverResponse.isValid(NONCE_VALIDITY_MS)) {
            throw new SecurityException("Réponse serveur expirée");
        }

        if (serverResponse.getPublicKeyBase64() == null) {
            throw new IllegalArgumentException("Clé publique serveur manquante");
        }

        try {
            // 1. Décoder clé publique RSA du serveur depuis Base64
            byte[] decodedKey = Base64.getDecoder().decode(serverResponse.getPublicKeyBase64());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey serverPublicKey = factory.generatePublic(spec);

            // 2. Générer clé AES-256 aléatoire
            // Utilise AESKeyGenerator de Personne 2
            this.negotiatedAESKey = AESKeyGenerator.generateKey();
            byte[] aesKeyBytes = this.negotiatedAESKey.getEncoded();

            System.out.println("[CLIENT] Clé AES-256 générée");

            // 3. Chiffrer clé AES avec clé publique RSA du serveur
            // Utilise RSAEncryptor de Personne 1
            byte[] encryptedAES = RSAEncryptor.encrypt(aesKeyBytes, serverPublicKey);

            System.out.println("[CLIENT] Clé AES chiffrée avec RSA");

            // 4. Créer requête phase 2 avec clé AES chiffrée
            HandshakeRequest request = new HandshakeRequest(clientNonce, encryptedAES);

            // 5. Marquer handshake comme complet
            this.handshakeComplete = true;

            System.out.println("[CLIENT] Clé AES chiffrée prête à envoyer");
            return request;

        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur handshake: " + e.getMessage());
            throw new SecurityException("Impossible de compléter handshake", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // GETTERS
    // ──────────────────────────────────────────────────────────────────

    /**
     * Récupérer clé AES négociée (utilisée pour SecureChannel)
     */
    public SecretKey getNegotiatedAESKey() {
        if (negotiatedAESKey == null) {
            throw new IllegalStateException("Handshake pas encore complet");
        }
        return negotiatedAESKey;
    }

    /**
     * Vérifier si handshake complété
     */
    public boolean isComplete() {
        return handshakeComplete;
    }
}