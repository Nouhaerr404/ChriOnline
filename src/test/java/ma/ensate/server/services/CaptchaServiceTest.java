package ma.ensate.server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class CaptchaServiceTest {

    @BeforeEach
    public void setUp() {
        // Nettoyer la mémoire avant chaque test
        CaptchaService.clearAll();
    }

    @Test
    public void testGenerateCaptcha() {
        CaptchaService.CaptchaResult result = CaptchaService.generateCaptcha();
        
        assertNotNull(result, "Le résultat de la génération de CAPTCHA ne doit pas être nul.");
        assertNotNull(result.captchaId, "L'Id du CAPTCHA ne doit pas être nul.");
        assertNotNull(result.captchaSessionToken, "Le jeton de session ne doit pas être nul.");
        assertNotNull(result.imageBase64, "L'image Base64 ne doit pas être nulle.");
        
        // Valider que l'image est un Base64 valide
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result.imageBase64), 
                "L'image renvoyée doit être correctement encodée en Base64.");
        
        // Vérifier l'existence en mémoire
        assertNotNull(CaptchaService.getCaptchasMap().get(result.captchaId), 
                "Le CAPTCHA généré doit être stocké en mémoire serveur.");
    }

    @Test
    public void testVerifyCaptchaSuccess() {
        CaptchaService.CaptchaResult result = CaptchaService.generateCaptcha();
        
        // Récupérer le code réel stocké côté serveur
        String realCode = CaptchaService.getCaptchasMap().get(result.captchaId).getCode();
        
        // Vérification avec les bons paramètres
        boolean success = CaptchaService.verifyCaptcha(result.captchaId, realCode, result.captchaSessionToken);
        assertTrue(success, "La validation avec le bon code et le bon token doit réussir.");
        
        // Vérifier qu'il a été purgé de la mémoire (Usage unique strict)
        assertNull(CaptchaService.getCaptchasMap().get(result.captchaId), 
                "Le CAPTCHA doit être immédiatement purgé après une validation réussie.");
    }

    @Test
    public void testVerifyCaptchaWrongCodeAndAttemptsLimit() {
        CaptchaService.CaptchaResult result = CaptchaService.generateCaptcha();
        String wrongCode = "WRONG_CODE";

        // Essai 1 : Incorrect
        boolean fail1 = CaptchaService.verifyCaptcha(result.captchaId, wrongCode, result.captchaSessionToken);
        assertFalse(fail1, "Un code incorrect doit être rejeté.");
        assertNotNull(CaptchaService.getCaptchasMap().get(result.captchaId), 
                "Le CAPTCHA ne doit pas être supprimé après un seul échec (essais < 3).");

        // Essai 2 : Incorrect
        boolean fail2 = CaptchaService.verifyCaptcha(result.captchaId, wrongCode, result.captchaSessionToken);
        assertFalse(fail2, "Un code incorrect doit être rejeté.");
        assertNotNull(CaptchaService.getCaptchasMap().get(result.captchaId), 
                "Le CAPTCHA ne doit pas être supprimé après deux échecs (essais < 3).");

        // Essai 3 : Incorrect (épuisement des tentatives)
        boolean fail3 = CaptchaService.verifyCaptcha(result.captchaId, wrongCode, result.captchaSessionToken);
        assertFalse(fail3, "Le troisième essai incorrect doit être rejeté.");
        
        // Vérifier qu'il a été purgé de la mémoire (3 essais épuisés)
        assertNull(CaptchaService.getCaptchasMap().get(result.captchaId), 
                "Le CAPTCHA doit être définitivement supprimé après 3 tentatives infructueuses.");
    }

    @Test
    public void testVerifyCaptchaWrongSessionToken() {
        CaptchaService.CaptchaResult result = CaptchaService.generateCaptcha();
        String realCode = CaptchaService.getCaptchasMap().get(result.captchaId).getCode();
        String wrongToken = "WRONG_SESSION_TOKEN";

        // Validation avec le bon code mais mauvais sessionToken (spoofing attempt)
        boolean success = CaptchaService.verifyCaptcha(result.captchaId, realCode, wrongToken);
        assertFalse(success, "La validation avec un token de session incorrect doit échouer.");
    }

    @Test
    public void testSlidingWindowRateLimiter() {
        String testIP = "192.168.4.15";

        // Envoyer 5 requêtes successives -> doit être accepté
        for (int i = 0; i < 5; i++) {
            assertTrue(CaptchaService.allowRequest(testIP), 
                    "La requête n°" + (i + 1) + " sous la limite doit être acceptée.");
        }

        // 6ème requête dans la même seconde -> doit être rejetée (Rate limited!)
        assertFalse(CaptchaService.allowRequest(testIP), 
                "La 6ème requête dans la fenêtre de 10 secondes doit être bloquée.");
    }
}
