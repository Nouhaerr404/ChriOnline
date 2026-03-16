package ma.ensate.client.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import ma.ensate.models.Produit;

public class ProduitDetailsView {

    @FXML private Label nomLabel;
    @FXML private Label categorieLabel;
    @FXML private Label prixLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label stockLabel;
    @FXML private ImageView produitImageView;

    private Produit currentProduit;

    /**
     * Initialise la vue avec les données du produit
     */
    public void setProduit(Produit produit) {
        this.currentProduit = produit;
        
        nomLabel.setText(produit.getNom());
        categorieLabel.setText(produit.getCategorie() != null ? produit.getCategorie().getNom() : "Sans catégorie");
        prixLabel.setText(String.format("%.2f MAD", produit.getPrix()));
        descriptionLabel.setText(produit.getDescription());
        stockLabel.setText(String.valueOf(produit.getStock()));

        if (produit.getImageUrl() != null && !produit.getImageUrl().isEmpty()) {
            try {
                produitImageView.setImage(new Image(produit.getImageUrl()));
            } catch (Exception e) {
                System.err.println("Erreur chargement image : " + e.getMessage());
            }
        }
    }

    @FXML
    private void retourner() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddToCart() {
        if (currentProduit == null) return;
        
        ma.ensate.models.Utilisateur u = ma.ensate.client.network.SessionManager.getInstance().getUtilisateur();
        if (u == null) {
            System.err.println("Utilisateur non connecté !");
            return;
        }

        new Thread(() -> {
            try {
                String data = u.getId() + "," + currentProduit.getId() + ",1";
                ma.ensate.protocol.Response r = ma.ensate.client.network.ClientTCP.getInstance()
                        .envoyerRequete(new ma.ensate.protocol.Request("AJOUTER_AU_PANIER", data, u.getSessionToken()));
                
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            r.isSuccess() ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR
                    );
                    alert.setTitle("Ajout au panier");
                    alert.setHeaderText(null);
                    alert.setContentText(r.isSuccess() ? "Produit ajouté au panier avec succès !" : "Erreur : " + r.getMessage());
                    alert.showAndWait();
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setContentText("Erreur réseau: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
}
