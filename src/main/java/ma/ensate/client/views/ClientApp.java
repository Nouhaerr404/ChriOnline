package ma.ensate.client.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.client.network.UDPNotificationClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée JavaFX pour l'application CLIENT uniquement.
 * Démarre sur la page landing (inscription/connexion client).
 * Aucun accès aux vues admin n'est possible depuis cette application.
 */
public class ClientApp extends Application {

    private static final Logger logger = LogManager.getLogger(ClientApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        try {
            ClientTCP.getInstance().connecter();
            logger.info("Connexion serveur etablie (Client)");
        } catch (Exception e) {
            logger.error("Serveur inaccessible : " + e.getMessage());
        }

        UDPNotificationClient udpClient = UDPNotificationClient.demarrer();
        SessionManager.getInstance().setUdpPort(udpClient.getUdpPort());
        logger.info("Système de notifications UDP démarré (Client)");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ma/ensate/fxml/landing.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("ChriOnline - E-Commerce Client");
        primaryStage.setScene(new Scene(root, 1280, 800));
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        ClientTCP.getInstance().deconnecter();
        logger.info("Application Client fermée.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
