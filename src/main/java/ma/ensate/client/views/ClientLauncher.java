package ma.ensate.client.views;

/**
 * Launcher pour l'application CLIENT uniquement.
 * Nécessaire pour contourner la restriction JavaFX qui empêche
 * le lancement direct depuis une classe qui étend Application dans un fat JAR.
 */
public class ClientLauncher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
