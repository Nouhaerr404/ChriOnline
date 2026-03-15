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
        // Personne 3 implements the cart logic
        System.out.println("Demande d'ajout au panier : " + currentProduit.getNom());
        // TODO: Appeler le service Panier de Personne 3
    }
}
