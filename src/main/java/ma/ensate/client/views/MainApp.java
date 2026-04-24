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

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        try {
            ClientTCP.getInstance().connecter();
            logger.info("Connexion serveur etablie");
        } catch (Exception e) {
            logger.error(" Serveur inaccessible : " + e.getMessage());

        }

        UDPNotificationClient udpClient = UDPNotificationClient.demarrer();
        SessionManager.getInstance().setUdpPort(udpClient.getUdpPort());
        logger.info("Système de notifications UDP démarré");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ma/ensate/fxml/landing.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("ChriOnline - Premium E-Commerce");
        primaryStage.setScene(new Scene(root, 1280, 800));
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {

        ClientTCP.getInstance().deconnecter();
        logger.info("Application fermée.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}