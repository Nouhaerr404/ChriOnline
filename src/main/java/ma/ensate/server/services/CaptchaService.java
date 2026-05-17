package ma.ensate.server.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de CAPTCHA personnalisé de niveau production.
 * Il assure la génération d'images distordues hautement sécurisées,
 * le stockage thread-safe lié à un token de session généré côté serveur,
 * la vérification atomique avec limite stricte à 3 essais,
 * et une protection de débit de type Sliding Window par IP.
 */
public class CaptchaService {

    private static final Logger logger = LogManager.getLogger(CaptchaService.class);
    private static final SecureRandom random = new SecureRandom();

    // Dictionnaire des CAPTCHAs actifs
    private static final Map<String, CaptchaEntry> captchas = new ConcurrentHashMap<>();

    // Dictionnaire des requêtes pour le Sliding Window Rate Limiter
    private static final Map<String, List<Long>> rateLimits = new ConcurrentHashMap<>();

    // Caractères sûrs pour le CAPTCHA (exclusion des ambigus: 0, O, I, 1)
    private static final char[] CAPTCHA_CHARS = 
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    // Planificateur pour nettoyer périodiquement les captchas expirés
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread t = new Thread(runnable, "CaptchaCleanupThread");
        t.setDaemon(true);
        return t;
    });

    static {
        // Purger les captchas expirés toutes les minutes
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            int countBefore = captchas.size();
            captchas.entrySet().removeIf(entry -> now > entry.getValue().getExpiryTime());
            int purged = countBefore - captchas.size();
            if (purged > 0) {
                logger.info("Nettoyage périodique : " + purged + " CAPTCHAs expirés purgés de la mémoire.");
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Structure de stockage interne d'un CAPTCHA généré par le serveur.
     */
    public static class CaptchaEntry {
        private final String captchaId;
        private final String code;
        private final String sessionToken;
        private final long expiryTime;
        private final AtomicInteger attempts;

        public CaptchaEntry(String captchaId, String code, String sessionToken, long expiryTime) {
            this.captchaId = captchaId;
            this.code = code;
            this.sessionToken = sessionToken;
            this.expiryTime = expiryTime;
            this.attempts = new AtomicInteger(0);
        }

        public String getCaptchaId() { return captchaId; }
        public String getCode() { return code; }
        public String getSessionToken() { return sessionToken; }
        public long getExpiryTime() { return expiryTime; }
        
        public int getAttempts() { return attempts.get(); }
        public int incrementAttempts() { return attempts.incrementAndGet(); }
    }

    /**
     * Modèle de résultat renvoyé par la génération de CAPTCHA.
     */
    public static class CaptchaResult {
        public final String captchaId;
        public final String imageBase64;
        public final String captchaSessionToken;

        public CaptchaResult(String captchaId, String imageBase64, String captchaSessionToken) {
            this.captchaId = captchaId;
            this.imageBase64 = imageBase64;
            this.captchaSessionToken = captchaSessionToken;
        }
    }

    /**
     * Vérifie la validité d'une demande de CAPTCHA sous l'algorithme Sliding Window.
     * Limite : max 5 requêtes par 10 secondes.
     */
    public static boolean allowRequest(String clientIP) {
        long now = System.currentTimeMillis();
        long windowMs = 10000; // 10 secondes
        int maxRequests = 5;

        List<Long> timestamps = rateLimits.computeIfAbsent(clientIP, k -> new CopyOnWriteArrayList<>());
        
        // Supprimer les timestamps en dehors de la fenêtre glissante
        timestamps.removeIf(t -> (now - t) > windowMs);

        if (timestamps.size() < maxRequests) {
            timestamps.add(now);
            return true;
        }
        logger.warn("Rate limiting activé pour l'IP : " + clientIP + " (Tentatives trop fréquentes de CAPTCHA)");
        return false;
    }

    /**
     * Génère un nouveau CAPTCHA, crée son token de session associé et retourne le Base64 de l'image.
     */
    public static CaptchaResult generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String sessionToken = UUID.randomUUID().toString();

        // 1. Génération du code aléatoire (6 à 7 caractères)
        int length = 6 + random.nextInt(2);
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            codeBuilder.append(CAPTCHA_CHARS[random.nextInt(CAPTCHA_CHARS.length)]);
        }
        String code = codeBuilder.toString();

        // 2. Génération de l'image de CAPTCHA déformée
        String imageBase64 = generateDistortedImage(code);

        // 3. Stockage avec expiration (2 minutes)
        long expiryTime = System.currentTimeMillis() + (2 * 60 * 1000);
        captchas.put(captchaId, new CaptchaEntry(captchaId, code, sessionToken, expiryTime));

        logger.info("Nouveau CAPTCHA généré [Id: " + captchaId + " | Longueur: " + length + "]");
        return new CaptchaResult(captchaId, imageBase64, sessionToken);
    }

    /**
     * Valide de manière thread-safe et robuste le code CAPTCHA fourni.
     * Nettoie immédiatement le CAPTCHA si correct ou après 3 tentatives.
     */
    public static boolean verifyCaptcha(String captchaId, String input, String sessionToken) {
        if (captchaId == null || input == null || sessionToken == null) {
            return false;
        }

        CaptchaEntry entry = captchas.get(captchaId);
        if (entry == null) {
            logger.warn("Tentative de validation de CAPTCHA introuvable ou déjà expiré/supprimé [Id: " + captchaId + "]");
            return false;
        }

        // 1. Vérification d'expiration
        if (System.currentTimeMillis() > entry.getExpiryTime()) {
            logger.warn("Validation échouée : CAPTCHA expiré [Id: " + captchaId + "]");
            captchas.remove(captchaId);
            return false;
        }

        // 2. Vérification de correspondance (code et token de session serveur)
        boolean valid = entry.getCode().equalsIgnoreCase(input.trim()) &&
                        entry.getSessionToken().equals(sessionToken);

        if (valid) {
            logger.info("CAPTCHA validé avec succès [Id: " + captchaId + "]");
            captchas.remove(captchaId);
            return true;
        }

        // 3. Incrément atomique et gestion des tentatives max (3)
        int attempts = entry.incrementAttempts();
        logger.warn("Code CAPTCHA incorrect pour l'id: " + captchaId + " (Essai n°" + attempts + "/3)");

        if (attempts >= 3) {
            logger.warn("Limite de tentatives épuisée pour le CAPTCHA [Id: " + captchaId + "]. Purge de la mémoire.");
            captchas.remove(captchaId);
        }

        return false;
    }

    /**
     * Génère l'image distordue avec AWT et Java2D.
     */
    private static String generateDistortedImage(String code) {
        int width = 220;
        int height = 65;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Anti-aliasing premium pour le lissage
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fond sombre premium (matching ChriOnline style)
        g2d.setColor(new Color(30, 30, 47));
        g2d.fillRect(0, 0, width, height);

        // 1. Ajouter du bruit de fond (petits points poivre et sel)
        g2d.setColor(new Color(90, 90, 110, 100));
        for (int i = 0; i < 250; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = 1 + random.nextInt(3);
            g2d.fillOval(x, y, size, size);
        }

        // 2. Dessiner des lignes de bruit incurvées et déformantes
        g2d.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 4; i++) {
            g2d.setColor(new Color(60 + random.nextInt(100), 100 + random.nextInt(100), 200 + random.nextInt(55), 180));
            int x1 = random.nextInt(width / 4);
            int y1 = random.nextInt(height);
            int x2 = width - random.nextInt(width / 4);
            int y2 = random.nextInt(height);
            int ctrlX = width / 2 + random.nextInt(40) - 20;
            int ctrlY = height / 2 + random.nextInt(40) - 20;
            g2d.draw(new java.awt.geom.QuadCurve2D.Float(x1, y1, ctrlX, ctrlY, x2, y2));
        }

        // 3. Dessiner chaque lettre du code avec des rotations et translations aléatoires
        Font font = new Font("Arial", Font.BOLD, 30);
        g2d.setFont(font);

        int charWidth = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);

            // Couleur aléatoire lumineuse pour chaque caractère
            g2d.setColor(new Color(100 + random.nextInt(120), 150 + random.nextInt(100), 220 + random.nextInt(35)));

            // Sauvegarder la transformation originale
            AffineTransform origTransform = g2d.getTransform();

            // Rotation entre -25 et +25 degrés
            double angle = (random.nextDouble() * 50 - 25) * Math.PI / 180.0;
            int x = 20 + i * charWidth + random.nextInt(8) - 4;
            int y = height / 2 + 10 + random.nextInt(8) - 4;

            // Appliquer la rotation et le cisaillement local
            g2d.translate(x, y);
            g2d.rotate(angle);
            g2d.shear(random.nextDouble() * 0.2 - 0.1, random.nextDouble() * 0.2 - 0.1);

            g2d.drawString(String.valueOf(ch), 0, 0);

            // Restaurer la transformation originale
            g2d.setTransform(origTransform);
        }

        // 4. Ajouter de légères lignes d'interférence en premier plan
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(255, 255, 255, 60));
        for (int i = 0; i < 3; i++) {
            int y = random.nextInt(height);
            g2d.drawLine(0, y, width, y + random.nextInt(20) - 10);
        }

        g2d.dispose();

        // 5. Encodage en Base64 PNG
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de l'image de CAPTCHA : " + e.getMessage());
            return "";
        }
    }

    /**
     * Méthode d'accès directe pour les tests unitaires.
     */
    public static Map<String, CaptchaEntry> getCaptchasMap() {
        return captchas;
    }

    /**
     * Réinitialise les maps (utile pour nettoyer les tests unitaires).
     */
    public static void clearAll() {
        captchas.clear();
        rateLimits.clear();
    }
}
