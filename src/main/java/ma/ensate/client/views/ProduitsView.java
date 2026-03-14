package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;

import java.util.Collections;
import java.util.List;

public class ProduitsView {

    @FXML
    private ComboBox<Categorie> categoryComboBox;

    @FXML
    private FlowPane productsFlowPane;

    @FXML
    public void initialize() {
        loadCategories();
        loadAllProducts();
    }

    @FXML
    private void goToCart() {
        // Personne 3 implements this
        System.out.println("Navigation vers le Panier...");
    }

    @FXML
    private void goToOrders() {
        // Personne 4 implements this
        System.out.println("Navigation vers les Commandes...");
    }

    @FXML
    private void handleLogout() {
        try {
            // Informer le serveur de la déconnexion
            Utilisateur current = SessionManager.getInstance().getUtilisateur();
            if (current != null) {
                ClientTCP.getInstance().envoyerRequeteSecurisee("LOGOUT", current.getId());
            }
            
            // Effacer la session locale
            SessionManager.getInstance().clear();
            
            // Retourner à la page de connexion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productsFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 600)); // Garder la taille de la page de login
            stage.setTitle("ChriOnline — Connexion");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        new Thread(() -> {
            List<Categorie> categories = fetchCategoriesFromServer();
            Platform.runLater(() -> {
                categoryComboBox.getItems().clear();
                categoryComboBox.getItems().add(new Categorie(0, "Toutes les catégories"));
                categoryComboBox.getItems().addAll(categories);
                categoryComboBox.getSelectionModel().selectFirst();
            });
        }).start();
    }

    private void loadAllProducts() {
        new Thread(() -> {
            List<Produit> produits = fetchProductsFromServer();
            Platform.runLater(() -> displayProducts(produits));
        }).start();
    }

    @FXML
    private void handleCategoryFilter() {
        Categorie selected = categoryComboBox.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == 0) {
            loadAllProducts();
        } else {
            new Thread(() -> {
                List<Produit> produits = fetchProductsByCategoryFromServer(selected.getId());
                Platform.runLater(() -> displayProducts(produits));
            }).start();
        }
    }

    private void displayProducts(List<Produit> produits) {
        productsFlowPane.getChildren().clear();
        for (Produit produit : produits) {
            productsFlowPane.getChildren().add(createProductCard(produit));
        }
    }

    private VBox createProductCard(Produit produit) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setPrefSize(150, 200);

        Label nameLabel = new Label(produit.getNom());
        nameLabel.getStyleClass().add("product-name");

        Label priceLabel = new Label(String.format("%.2f MAD", produit.getPrix()));
        priceLabel.getStyleClass().add("product-price");

        Label stockLabel = new Label("Stock: " + produit.getStock());
        stockLabel.getStyleClass().add("product-stock");

        card.getChildren().addAll(nameLabel, priceLabel, stockLabel);

        card.setOnMouseClicked(event -> {
            System.out.println("Produit cliqué : " + produit.getNom());
            // TODO: Naviguer vers les détails du produit
        });

        return card;
    }

    // =============================================
    // NETWORK METHODS (Moved from client-side service)
    // =============================================

    @SuppressWarnings("unchecked")
    private List<Produit> fetchProductsFromServer() {
        try {
            Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_ALL_PRODUCTS", null);
            if (response.isSuccess()) {
                return (List<Produit>) response.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Categorie> fetchCategoriesFromServer() {
        try {
            Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_ALL_CATEGORIES", null);
            if (response.isSuccess()) {
                return (List<Categorie>) response.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Produit> fetchProductsByCategoryFromServer(int categoryId) {
        try {
            Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_BY_CATEGORY", categoryId);
            if (response.isSuccess()) {
                return (List<Produit>) response.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
