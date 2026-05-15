package ma.ensate.client.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée JavaFX pour l'application ADMIN uniquement.
 * Démarre directement sur la page de connexion admin (challenge RSA).
 * Aucun accès aux vues client (landing, inscription, panier, etc.) 
 * n'est possible depuis cette application.
 */
public class AdminApp extends Application {

    private static final Logger logger = LogManager.getLogger(AdminApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        try {
            ClientTCP.getInstance().connecter();
            logger.info("Connexion serveur etablie (Admin)");
        } catch (Exception e) {
            logger.error("Serveur inaccessible : " + e.getMessage());
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ma/ensate/fxml/admin_login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("ChriOnline - Administration");
        primaryStage.setScene(new Scene(root, 1280, 800));
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        ClientTCP.getInstance().deconnecter();
        logger.info("Application Admin fermée.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
