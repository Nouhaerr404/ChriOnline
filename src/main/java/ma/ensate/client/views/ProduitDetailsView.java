package ma.ensate.client.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import ma.ensate.client.network.SessionManager;
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
            String target = SessionManager.getInstance().estAdmin()
                ? "/ma/ensate/fxml/admin_produits.fxml"
                : "/ma/ensate/fxml/produits.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
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
        if (u == null) return;

        new Thread(() -> {
            try {
                String data = u.getId() + "," + currentProduit.getId() + ",1";
                ma.ensate.protocol.Response r = ma.ensate.client.network.ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("AJOUTER_AU_PANIER", data);
                
                javafx.application.Platform.runLater(() -> {
                    ma.ensate.client.utils.NotificationUtils.showToast(
                        (Stage) nomLabel.getScene().getWindow(), 
                        r.isSuccess() ? "Produit ajouté au panier !" : "Erreur: " + r.getMessage()
                    );
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleBuyNow() {
        if (currentProduit == null) return;
        
        ma.ensate.models.Utilisateur u = ma.ensate.client.network.SessionManager.getInstance().getUtilisateur();
        if (u == null) return;

        new Thread(() -> {
            try {
                String data = u.getId() + "," + currentProduit.getId() + ",1";
                ma.ensate.protocol.Response r = ma.ensate.client.network.ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("AJOUTER_AU_PANIER", data);
                
                javafx.application.Platform.runLater(() -> {
                    if (r.isSuccess()) {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/panier.fxml"));
                            Parent root = loader.load();
                            Stage stage = (Stage) nomLabel.getScene().getWindow();
                            stage.getScene().setRoot(root);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        ma.ensate.client.utils.NotificationUtils.showToast(
                            (Stage) nomLabel.getScene().getWindow(), 
                            "Erreur: " + r.getMessage()
                        );
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}
