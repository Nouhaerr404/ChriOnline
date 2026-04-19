package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;
import ma.ensate.models.Utilisateur;
import ma.ensate.client.utils.NotificationUtils;
import ma.ensate.protocol.Response;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ProduitsView {

    @FXML
    private ComboBox<Categorie> categoryComboBox;

    @FXML
    private FlowPane productsFlowPane;

    @FXML
    private Label profileInitialLabel;

    @FXML
    private Label profileHoverLabel;

    @FXML
    private Label productCountLabel;

    @FXML
    public void initialize() {
        refreshCatalog();
        if (profileInitialLabel != null) {
            profileInitialLabel.setText(buildUserInitial());
        }
        if (profileHoverLabel != null) {
            profileHoverLabel.setManaged(false);
            profileHoverLabel.setVisible(false);
        }
    }

    @FXML
    public void refreshCatalog() {
        loadCategories();
        loadAllProducts();
    }

    @FXML
    private void goToProfil() {
        System.out.println("[Navigation] Boutique -> Profil");
        navigateToProfil();
    }

    @FXML
    private void goToProfilMouse(javafx.scene.input.MouseEvent event) { navigateToProfil(); }

    private void navigateToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/profil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productsFlowPane.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Mon Profil");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showProfileText() {
        if (profileHoverLabel == null) {
            return;
        }
        profileHoverLabel.setManaged(true);
        profileHoverLabel.setVisible(true);
    }

    @FXML
    private void hideProfileText() {
        if (profileHoverLabel == null) {
            return;
        }
        profileHoverLabel.setVisible(false);
        profileHoverLabel.setManaged(false);
    }

    @FXML
    private void goToPanier() {
        Utilisateur u = SessionManager.getInstance().getUtilisateur();
        if (u == null) {
            NotificationUtils.showToast((Stage) productsFlowPane.getScene().getWindow(), "Veuillez vous connecter pour voir votre panier.");
            return;
        }
        Stage stage = (Stage) productsFlowPane.getScene().getWindow();
        new PanierView(stage, ClientTCP.getInstance(), u.getId(), u.getSessionToken()).afficher();
    }

    @FXML
    private void goToOrders() {
        Utilisateur u = SessionManager.getInstance().getUtilisateur();
        if (u == null) {
            NotificationUtils.showToast((Stage) productsFlowPane.getScene().getWindow(), "Veuillez vous connecter pour voir vos commandes.");
            return;
        }
        Stage stage = (Stage) productsFlowPane.getScene().getWindow();
        new HistoriqueView(stage, ClientTCP.getInstance(), u.getId(), u.getSessionToken()).afficher();
    }

    @FXML
    private void handleLogout() {
        try {
            Utilisateur current = SessionManager.getInstance().getUtilisateur();
            if (current != null) {
                ClientTCP.getInstance().envoyerRequeteSecurisee("LOGOUT", current.getId());
            }

            SessionManager.getInstance().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productsFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 600));
            stage.setTitle("ChriOnline - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        new Thread(() -> {
            List<Categorie> categories = fetchCategoriesFromServer();
            Platform.runLater(() -> {
                categoryComboBox.getItems().clear();
                categoryComboBox.getItems().add(new Categorie(0, "Toutes les categories"));
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
        if (productCountLabel != null) {
            productCountLabel.setText(produits.size() + " PRODUITS TROUVÉS");
        }
        for (Produit produit : produits) {
            productsFlowPane.getChildren().add(createProductCard(produit));
        }
    }

    private VBox createProductCard(Produit produit) {
        VBox card = new VBox(14);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(320);
        card.setMinWidth(320);

        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.getStyleClass().add("product-image-placeholder");
        ImageView productImage = createProductImageView(produit);
        if (productImage != null) {
            imagePlaceholder.getChildren().add(productImage);
        } else {
            Label iconLabel = new Label(resolveCategoryGlyph(produit));
            iconLabel.getStyleClass().add("product-icon");
            imagePlaceholder.getChildren().add(iconLabel);
        }

        VBox details = new VBox(8);
        details.setFillWidth(true);

        Label categoryLabel = new Label(resolveCategoryName(produit));
        categoryLabel.getStyleClass().add("category-tag");

        Label nameLabel = new Label(produit.getNom());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);

        Label descriptionLabel = new Label(buildProductSnippet(produit));
        descriptionLabel.getStyleClass().add("product-description");
        descriptionLabel.setWrapText(true);

        HBox priceContainer = new HBox(12);
        priceContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label priceLabel = new Label(String.format("%.2f MAD", produit.getPrix()));
        priceLabel.getStyleClass().add("product-price");

        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);

        Label stockLabel = new Label("En stock: " + produit.getStock());
        stockLabel.getStyleClass().add("product-stock");
        stockLabel.setStyle("-fx-text-fill: " + (produit.getStock() > 0 ? "#5cff90" : "#ff7ea8"));

        priceContainer.getChildren().addAll(priceLabel, priceSpacer, stockLabel);

        Button addToCartBtn = new Button("AJOUTER AU PANIER");
        addToCartBtn.getStyleClass().add("product-action-btn");
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);
        addToCartBtn.setPrefHeight(46);
        addToCartBtn.setDisable(produit.getStock() <= 0);

        addToCartBtn.setOnAction(e -> {
            try {
                int userId = SessionManager.getInstance().getUserId();
                String data = userId + "," + produit.getId() + ",1";
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("AJOUTER_AU_PANIER", data);
                
                if (response.isSuccess()) {
                    NotificationUtils.showToast((Stage) card.getScene().getWindow(), "Produit ajouté au panier !");
                } else {
                    NotificationUtils.showToast((Stage) card.getScene().getWindow(), "Erreur: " + response.getMessage());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        details.getChildren().addAll(categoryLabel, nameLabel, descriptionLabel, priceContainer, addToCartBtn);

        card.getChildren().addAll(imagePlaceholder, details);
        
        // --- Animations ---
        card.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        card.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        card.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Button) return; // Prevent navigation on button click if preferred
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produit_details.fxml"));
                Parent root = loader.load();

                ProduitDetailsView controller = loader.getController();
                controller.setProduit(produit);

                Stage stage = (Stage) productsFlowPane.getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("ChriOnline - " + produit.getNom());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return card;
    }

    private ImageView createProductImageView(Produit produit) {
        String imageSource = resolveProductImageSource(produit);
        if (imageSource == null) {
            return null;
        }

        try {
            Image image = new Image(imageSource, true);
            if (image.isError()) {
                return null;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(300);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("product-image");
            return imageView;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveProductImageSource(Produit produit) {
        if (produit != null && produit.getImageUrl() != null && !produit.getImageUrl().isBlank()) {
            String imageUrl = produit.getImageUrl().trim();
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("file:")) {
                return imageUrl;
            }

            File directFile = new File(imageUrl);
            if (directFile.exists()) {
                return directFile.toURI().toString();
            }

            File uploadsFile = new File("uploads/produits", imageUrl);
            if (uploadsFile.exists()) {
                return uploadsFile.toURI().toString();
            }
        }

        // Fallback for online pics as requested by user
        String category = resolveCategoryName(produit).toLowerCase();
        if (category.contains("pc") || category.contains("laptop") || category.contains("ordinateur")) {
            return "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500&q=80";
        } else if (category.contains("camera") || category.contains("photo")) {
            return "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500&q=80";
        } else if (category.contains("audio") || category.contains("son") || category.contains("casque")) {
            return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&q=80";
        } else if (category.contains("tel") || category.contains("phone") || category.contains("mobile")) {
            return "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&q=80";
        }
        
        return "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&q=80"; // Default product image
    }

    private String resolveCategoryName(Produit produit) {
        if (produit != null && produit.getCategorie() != null && produit.getCategorie().getNom() != null
                && !produit.getCategorie().getNom().isBlank()) {
            return produit.getCategorie().getNom().toUpperCase();
        }
        return "COLLECTION";
    }

    private String resolveCategoryGlyph(Produit produit) {
        String category = produit != null && produit.getCategorie() != null ? produit.getCategorie().getNom() : "";
        if (category == null) {
            return "◈";
        }

        String normalized = category.toLowerCase();
        if (normalized.contains("camera") || normalized.contains("photo") || normalized.contains("image")) {
            return "◉";
        }
        if (normalized.contains("audio") || normalized.contains("son")) {
            return "◌";
        }
        if (normalized.contains("pc") || normalized.contains("ordinateur") || normalized.contains("laptop")) {
            return "▣";
        }
        return "◈";
    }

    private String buildProductSnippet(Produit produit) {
        String description = produit != null ? produit.getDescription() : null;
        if (description == null || description.isBlank()) {
            return "Pièce sélectionnée pour une vitrine plus premium, sans changement de logique métier.";
        }

        String cleaned = description.trim();
        return cleaned.length() > 120 ? cleaned.substring(0, 117) + "..." : cleaned;
    }

    private String buildUserInitial() {
        String nomUtilisateur = SessionManager.getInstance().getNomUtilisateur();
        if (nomUtilisateur == null || nomUtilisateur.isBlank()) {
            return "U";
        }
        return nomUtilisateur.trim().substring(0, 1).toUpperCase();
    }

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
