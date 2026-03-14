package ma.ensate.client.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Connexion au serveur TCP
        try {
            ClientTCP.getInstance().connecter();
            logger.info("Connexion serveur établie");
        } catch (Exception e) {
            logger.error(" Serveur inaccessible : " + e.getMessage());
            // L'app démarre quand même
            // les vues afficheront l'erreur si besoin
        }

        // Charger la vue Login
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ma/ensate/fxml/login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("ChriOnline");
        primaryStage.setScene(new Scene(root, 500, 500));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Fermer la connexion TCP proprement
        ClientTCP.getInstance().deconnecter();
        logger.info("Application fermée.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}