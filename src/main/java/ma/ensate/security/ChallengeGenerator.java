package ma.ensate.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Générateur de challenges aléatoires pour l'authentification challenge-response
 */
public class ChallengeGenerator {

    private static final int CHALLENGE_BYTES = 32; // 256 bits
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Génère un challenge aléatoire encodé en Base64
     * @return String représentant le challenge en Base64
     */
    public static String generateChallenge() {
        byte[] random = new byte[CHALLENGE_BYTES];
        secureRandom.nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }

    /**
     * Génère un challenge avec une taille personnalisée
     * @param bytes nombre d'octets aléatoires
     * @return String représentant le challenge en Base64
     */
    public static String generateChallenge(int bytes) {
        byte[] random = new byte[bytes];
        secureRandom.nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }
}
