package ma.ensate.security;

import java.security.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service d'authentification admin par challenge-response RSA
 * Gère la génération et vérification des challenges
 */
public class AdminAuthService {

    // Stockage temporaire des challenges avec expiration (30 secondes)
    private static final long CHALLENGE_EXPIRATION_MS = 30_000; // 30 secondes
    private static final ConcurrentHashMap<String, ChallengeData> activeChallenges = new ConcurrentHashMap<>();

    /**
     * Génère et enregistre un nouveau challenge pour un admin
     * @param adminEmail email de l'admin
     * @return le challenge généré en Base64
     */
    public static String generateChallengeForAdmin(String adminEmail) {
        String challenge = ChallengeGenerator.generateChallenge();
        activeChallenges.put(challenge, new ChallengeData(adminEmail, System.currentTimeMillis()));
        return challenge;
    }

    /**
     * Vérifie si un challenge est valide et non expiré
     * @param challenge le challenge à vérifier
     * @param adminEmail email de l'admin
     * @return true si le challenge est valide
     */
    public static boolean isChallengeValid(String challenge, String adminEmail) {
        ChallengeData data = activeChallenges.get(challenge);
        if (data == null) {
            return false;
        }

        // Vérifier l'expiration
        long age = System.currentTimeMillis() - data.timestamp;
        if (age > CHALLENGE_EXPIRATION_MS) {
            activeChallenges.remove(challenge);
            return false;
        }

        // Vérifier que le challenge appartient au bon admin
        return data.adminEmail.equals(adminEmail);
    }

    /**
     * Vérifie la signature du challenge avec la clé publique de l'admin
     * @param challenge le challenge original
     * @param signatureBase64 la signature en Base64
     * @param publicKeyBase64 la clé publique en Base64
     * @return true si la signature est valide
     * @throws Exception en cas d'erreur de vérification
     */
    public static boolean verifySignature(String challenge, String signatureBase64, String publicKeyBase64) throws Exception {
        PublicKey publicKey = KeySerializer.deserializePublicKey(publicKeyBase64);
        return RSAVerifier.verifyBase64(challenge, signatureBase64, publicKey);
    }

    /**
     * Supprime un challenge après utilisation (anti-replay)
     * @param challenge le challenge à supprimer
     */
    public static void consumeChallenge(String challenge) {
        activeChallenges.remove(challenge);
    }

    /**
     * Nettoie les challenges expirés (appel périodique recommandé)
     */
    public static void cleanupExpiredChallenges() {
        long now = System.currentTimeMillis();
        activeChallenges.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().timestamp;
            return age > CHALLENGE_EXPIRATION_MS;
        });
    }

    /**
     * Classe interne pour stocker les métadonnées du challenge
     */
    private static class ChallengeData {
        String adminEmail;
        long timestamp;

        ChallengeData(String adminEmail, long timestamp) {
            this.adminEmail = adminEmail;
            this.timestamp = timestamp;
        }
    }
}
