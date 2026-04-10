package ma.ensate.server.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test JUnit 5 pour valider la sécurité des sessions et de l'IP Binding.
 */
public class TestSessionSecurity {

    @Test
    @DisplayName("Vérifie que le Session-IP Binding autorise la même IP")
    void testSessionAllowedForSameIP() {
        String token = "token-valide-1";
        int userId = 1;
        String ip = "192.168.1.10";

        SessionManager.startSession(token, userId, ip);
        SessionManager.SessionResult result = SessionManager.evaluerEtRegenerer(token, ip);

        assertTrue(result.isValid, "L'accès devrait être autorisé pour la même IP");
    }

    @Test
    @DisplayName("Vérifie que le Session-IP Binding bloque et invalide une IP différente")
    void testSessionBlockedAndInvalidatedForDifferentIP() {
        String token = "token-valide-2";
        int userId = 2;
        String originalIP = "192.168.1.10";
        String attackerIP = "1.2.3.4";

        // Étape 1 : Connexion initiale
        SessionManager.startSession(token, userId, originalIP);

        // Étape 2 : Tentative d'attaque
        SessionManager.SessionResult resultAttacker = SessionManager.evaluerEtRegenerer(token, attackerIP);
        
        // Assertions
        assertFalse(resultAttacker.isValid, "L'accès devrait être refusé pour une IP différente");
        assertTrue(resultAttacker.errorMessage.contains("IP a changé"), "Le message devrait mentionner le changement d'IP");

        // Étape 3 : Vérifier que le jeton a été supprimé (Alerte Sécurité)
        SessionManager.SessionResult resultRetryOriginal = SessionManager.evaluerEtRegenerer(token, originalIP);
        assertFalse(resultRetryOriginal.isValid, "La session devrait avoir été invalidée après l'attaque");
    }

    @Test
    @DisplayName("Vérifie la logique des IPs internes")
    void testInternalIPLogic() {
        // Validation des IPs autorisées (Internes)
        assertTrue(checkIP("127.0.0.1"));
        assertTrue(checkIP("::1"));
        assertTrue(checkIP("192.168.1.1"));
        assertTrue(checkIP("10.0.0.1"));
        assertTrue(checkIP("172.16.0.1"));
        assertTrue(checkIP("172.31.255.255"));

        // Validation des IPs rejetées (Externes)
        assertFalse(checkIP("8.8.8.8"));
        assertFalse(checkIP("1.1.1.1"));
        assertFalse(checkIP("172.32.0.1"));
    }

    // Logique copiée pour tester la cohérence (doit être synchrone avec UserService.isInternalIP)
    private boolean checkIP(String ip) {
        return ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")
                || ip.equals("127.0.0.1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1");
    }
}
