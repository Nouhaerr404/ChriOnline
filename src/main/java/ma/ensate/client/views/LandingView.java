package ma.ensate.client.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;

public class LandingView {

    @FXML
    private void goToCart(MouseEvent event) {
        switchScene(event, "/ma/ensate/fxml/login.fxml", "Connexion requise");
    }

    @FXML
    private void handleLogin(javafx.event.Event event) {
        switchScene(event, "/ma/ensate/fxml/login.fxml", "ChriOnline - Connexion");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        switchScene(event, "/ma/ensate/fxml/register.fxml", "ChriOnline - Inscription");
    }

    private void switchScene(Object event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = null;
            if (event instanceof javafx.event.Event) {
                Object source = ((javafx.event.Event) event).getSource();
                if (source instanceof Node) {
                    stage = (Stage) ((Node) source).getScene().getWindow();
                }
            }
            if (stage != null) {
                stage.getScene().setRoot(root);
                stage.setTitle(title);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
