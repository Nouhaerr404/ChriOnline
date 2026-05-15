package ma.ensate.client.views;

/**
 * Launcher pour l'application ADMIN uniquement.
 * Nécessaire pour contourner la restriction JavaFX qui empêche
 * le lancement direct depuis une classe qui étend Application dans un fat JAR.
 */
public class AdminLauncher {
    public static void main(String[] args) {
        AdminApp.main(args);
    }
}
