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
    private void goToCatalog(MouseEvent event) {
        switchScene(event, "/ma/ensate/fxml/produits.fxml", "ChriOnline - Catalogue");
    }

    @FXML
    private void goToCatalogBtn(ActionEvent event) {
        switchScene(event, "/ma/ensate/fxml/produits.fxml", "ChriOnline - Catalogue");
    }

    @FXML
    private void goToCart(MouseEvent event) {
        switchScene(event, "/ma/ensate/fxml/login.fxml", "Connexion requise");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        switchScene(event, "/ma/ensate/fxml/login.fxml", "ChriOnline - Connexion");
    }

    private void switchScene(Object event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage;
            if (event instanceof ActionEvent) {
                stage = (Stage) ((Node) ((ActionEvent) event).getSource()).getScene().getWindow();
            } else {
                stage = (Stage) ((Node) ((MouseEvent) event).getSource()).getScene().getWindow();
            }
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
