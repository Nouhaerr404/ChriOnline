package ma.ensate.util;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Properties;

/**
 * Charge le fichier .env depuis la racine du projet.
 * Stratégie de recherche (ordre de priorité) :
 *  1. Dossier de travail courant (user.dir)
 *  2. Dossier parent du JAR / classes (pour exécution Maven ou JAR)
 *  3. Classpath (si .env est placé dans src/main/resources)
 */
public class ConfigLoader {

    private static final Properties properties = new Properties();

    static {
        boolean loaded = false;

        // 1) user.dir (racine du projet quand on lance depuis l'IDE ou mvn)
        Path workDir = Paths.get(System.getProperty("user.dir"), ".env");
        loaded = tryLoad(workDir);

        // 2) Dossier parent du JAR / dossier target/classes
        if (!loaded) {
            try {
                Path jarDir = Paths.get(
                        ConfigLoader.class.getProtectionDomain()
                                .getCodeSource().getLocation().toURI()
                ).getParent();
                if (jarDir != null) {
                    // remonter depuis target/classes → target → project root
                    loaded = tryLoad(jarDir.resolve(".env"));
                    if (!loaded) loaded = tryLoad(jarDir.getParent().resolve(".env"));
                    if (!loaded) loaded = tryLoad(jarDir.getParent().getParent().resolve(".env"));
                }
            } catch (URISyntaxException ignored) {}
        }

        // 3) Classpath (src/main/resources/.env)
        if (!loaded) {
            try (InputStream is = ConfigLoader.class.getResourceAsStream("/.env")) {
                if (is != null) {
                    properties.load(is);
                    loaded = true;
                    System.out.println("[ConfigLoader] .env chargé depuis le classpath.");
                }
            } catch (IOException ignored) {}
        }

        if (!loaded) {
            System.err.println("[ConfigLoader] AVERTISSEMENT : .env introuvable. " +
                    "Les valeurs par défaut seront utilisées.");
        }
    }

    private static boolean tryLoad(Path path) {
        if (Files.exists(path)) {
            try (InputStream is = new FileInputStream(path.toFile())) {
                properties.load(is);
                System.out.println("[ConfigLoader] .env chargé depuis : " + path.toAbsolutePath());
                return true;
            } catch (IOException e) {
                System.err.println("[ConfigLoader] Impossible de lire : " + path + " — " + e.getMessage());
            }
        }
        return false;
    }

    public static String get(String key, String defaultValue) {
        // Priorité : .env local > variable d'environnement système (pour éviter les conflits locaux)
        String prop = properties.getProperty(key);
        if (prop != null) {
            return prop;
        }
        
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("[ConfigLoader] Valeur entière invalide pour '" + key +
                        "'. Défaut utilisé : " + defaultValue);
            }
        }
        return defaultValue;
    }
}
