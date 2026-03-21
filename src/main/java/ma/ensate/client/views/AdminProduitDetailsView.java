package ma.ensate.client.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Produit;
import ma.ensate.protocol.Response;

import java.util.Optional;

public class AdminProduitDetailsView {

    @FXML private Label nomLabel;
    @FXML private Label categorieLabel;
    @FXML private Label prixLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label stockLabel;
    @FXML private ImageView produitImageView;

    private Produit currentProduit;

    public void setProduit(Produit produit) {
        this.currentProduit = produit;
        nomLabel.setText(produit.getNom());
        categorieLabel.setText(produit.getCategorie() != null ? produit.getCategorie().getNom() : "Sans categorie");
        prixLabel.setText(String.format("%.2f MAD", produit.getPrix()));
        descriptionLabel.setText(produit.getDescription());
        stockLabel.setText(String.valueOf(produit.getStock()));

        if (produit.getImageUrl() != null && !produit.getImageUrl().isBlank()) {
            try {
                produitImageView.setImage(new Image(produit.getImageUrl()));
            } catch (Exception ignored) {
            }
        }
    }

    @FXML
    private void retourner() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Dashboard Admin Produits");
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void modifierProduit() {
        if (currentProduit == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produit_form.fxml"));
            Parent root = loader.load();

            ProduitFormView controller = loader.getController();
            controller.setProduitToEdit(currentProduit);

            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Modifier produit");
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void supprimerProduit() {
        if (currentProduit == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation suppression");
        confirm.setHeaderText("Supprimer le produit: " + currentProduit.getNom());
        confirm.setContentText("Cette action est irreversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("DELETE_PRODUCT", currentProduit.getId());
                if (response.isSuccess()) {
                    javafx.application.Platform.runLater(this::retourner);
                } else {
                    javafx.application.Platform.runLater(() -> {
                        Alert error = new Alert(Alert.AlertType.ERROR, "Echec suppression: " + response.getMessage());
                        error.showAndWait();
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Erreur reseau: " + e.getMessage());
                    error.showAndWait();
                });
            }
        }).start();
    }
}
