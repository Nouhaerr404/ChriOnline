package ma.ensate.server.services;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaService {

    private static final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();
    private static final Random rng = new Random();
    private static final long TTL_MS = 3 * 60 * 1000L; // 3 minutes

    private static final int WIDTH  = 200;
    private static final int HEIGHT = 70;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private record CaptchaEntry(String code, long expiresAt) {}

    // ---------- API publique ----------

    /** Génère un captcha et retourne {id, base64PNG} */
    public static String[] generer() {
        purger();
        String code      = genererCode(6);
        String id        = UUID.randomUUID().toString();
        long   expiresAt = System.currentTimeMillis() + TTL_MS;
        store.put(id, new CaptchaEntry(code, expiresAt));
        String base64 = rendreEnBase64(code);
        return new String[]{id, base64};
    }

    /** Vérifie la réponse (insensible à la casse). Consomme le captcha. */
    public static boolean verifier(String id, String reponse) {
        if (id == null || reponse == null) return false;
        CaptchaEntry entry = store.remove(id);
        if (entry == null || System.currentTimeMillis() > entry.expiresAt()) return false;
        return entry.code().equalsIgnoreCase(reponse.trim());
    }

    // ---------- Génération du code ----------

    private static String genererCode(int longueur) {
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++)
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        return sb.toString();
    }

    // ---------- Rendu image ----------

    private static String rendreEnBase64(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fond dégradé
        GradientPaint bg = new GradientPaint(0, 0,
                new Color(230, 235, 255),
                WIDTH, HEIGHT,
                new Color(210, 220, 245));
        g.setPaint(bg);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Lignes de bruit
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 8; i++) {
            g.setColor(new Color(rng.nextInt(180), rng.nextInt(180), rng.nextInt(180), 120));
            g.drawLine(rng.nextInt(WIDTH), rng.nextInt(HEIGHT),
                       rng.nextInt(WIDTH), rng.nextInt(HEIGHT));
        }

        // Points de bruit
        for (int i = 0; i < 60; i++) {
            g.setColor(new Color(rng.nextInt(200), rng.nextInt(200), rng.nextInt(200), 150));
            int x = rng.nextInt(WIDTH);
            int y = rng.nextInt(HEIGHT);
            g.fillOval(x, y, 3, 3);
        }

        // Dessin des caractères (déformés individuellement)
        int startX = 15;
        int charW  = (WIDTH - 30) / code.length();

        for (int i = 0; i < code.length(); i++) {
            String letter = String.valueOf(code.charAt(i));

            // Police aléatoire parmi Bold / Italic / BoldItalic
            int style = new int[]{Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC}[rng.nextInt(3)];
            int size  = 28 + rng.nextInt(10); // 28–37px
            g.setFont(new Font("Arial", style, size));

            // Couleur sombre lisible
            g.setColor(new Color(
                    20  + rng.nextInt(80),
                    20  + rng.nextInt(80),
                    80  + rng.nextInt(120)));

            // Transformation : rotation légère + décalage vertical
            AffineTransform old = g.getTransform();
            int    cx    = startX + i * charW + charW / 2;
            int    cy    = HEIGHT / 2 + 8;
            double angle = Math.toRadians(-20 + rng.nextInt(41)); // -20° à +20°
            g.translate(cx, cy);
            g.rotate(angle);
            g.drawString(letter, -size / 4, size / 4);
            g.setTransform(old);
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private static void purger() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
    }
}
