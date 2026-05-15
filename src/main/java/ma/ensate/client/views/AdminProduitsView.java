package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AdminProduitsView {

    @FXML private ComboBox<Categorie> categoryComboBox;
    @FXML private TableView<Produit> productsTable;
    @FXML private TableColumn<Produit, Integer> idColumn;
    @FXML private TableColumn<Produit, String> nomColumn;
    @FXML private TableColumn<Produit, String> categoryColumn;
    @FXML private TableColumn<Produit, Double> prixColumn;
    @FXML private TableColumn<Produit, Integer> stockColumn;
    @FXML private TableColumn<Produit, Void> actionsColumn;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        nomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCategorie() != null ? data.getValue().getCategorie().getNom() : "Sans categorie"
        ));
        prixColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPrix()));
        stockColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStock()));
        productsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Produit selected = productsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openDetails(selected);
                }
            }
        });

        loadCategories();
        loadAllProducts();
    }

    @FXML
    private void handleCategoryFilter() {
        Categorie selected = categoryComboBox.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == 0) {
            loadAllProducts();
            return;
        }

        setStatus("Chargement des produits filtres...");
        new Thread(() -> {
            List<Produit> produits = fetchProductsByCategoryFromServer(selected.getId());
            Platform.runLater(() -> {
                displayProducts(produits);
                setStatus(produits.size() + " produit(s) affiche(s)");
            });
        }).start();
    }

    @FXML
    private void handleCommandesPlaceholder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_commandes.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Gestion des commandes (Admin)");
        } catch (Exception e) {
            setStatus("Erreur navigation commandes: " + e.getMessage());
        }
    }

    @FXML
    private void handleHistoriquePlaceholder() {
        setStatus("La vue historique admin sera integree par l'autre membre.");
    }

    // Nouveau bouton pour rediriger vers la gestion des utilisateurs
    @FXML
    private void handleDashboard() {
        ma.ensate.client.utils.NotificationUtils.showToast((Stage) productsTable.getScene().getWindow(), "Dashboard en cours de développement");
    }

    @FXML
    private void handleUtilisateurs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_utilisateurs.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) productsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Gestion des Utilisateurs");
        } catch (Exception e) {
            setStatus("Erreur navigation utilisateurs: " + e.getMessage());
        }
    }

    private void openDetails(Produit selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_produit_details.fxml"));
            Parent root = loader.load();
            AdminProduitDetailsView controller = loader.getController();
            controller.setProduit(selected);

            Stage stage = (Stage) productsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Details produit");
        } catch (Exception e) {
            setStatus("Erreur d'ouverture des details: " + e.getMessage());
        }
    }

    @FXML
    private void handleAjouterProduit() {
        openForm(null);
    }

    @FXML
    private void refreshProducts() {
        loadAllProducts();
    }

    @FXML
    private void handleSupprimer() {
        Produit selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Selectionnez un produit a supprimer");
            return;
        }
        deleteProduct(selected);
    }

    private void deleteProduct(Produit selected) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation suppression");
        confirm.setHeaderText("Supprimer le produit: " + selected.getNom());
        confirm.setContentText("Cette action est irreversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        setStatus("Suppression en cours...");
        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("DELETE_PRODUCT", selected.getId());
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        setStatus("Produit supprime avec succes");
                        handleCategoryFilter();
                    } else {
                        setStatus("Echec suppression: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Erreur reseau: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleLogout() {
        try {
            Utilisateur current = SessionManager.getInstance().getUtilisateur();
            if (current != null) {
                ClientTCP.getInstance().envoyerRequeteSecurisee("LOGOUT", current.getId());
            }
            SessionManager.getInstance().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Connexion Admin");
        } catch (Exception e) {
            setStatus("Erreur logout: " + e.getMessage());
        }
    }

    private void openForm(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produit_form.fxml"));
            Parent root = loader.load();
            ProduitFormView controller = loader.getController();
            if (produit != null) {
                controller.setProduitToEdit(produit);
            }

            Stage stage = (Stage) productsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(produit == null ? "ChriOnline - Ajouter produit" : "ChriOnline - Modifier produit");
        } catch (Exception e) {
            setStatus("Erreur ouverture formulaire: " + e.getMessage());
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
        setStatus("Chargement des produits...");
        new Thread(() -> {
            List<Produit> produits = fetchProductsFromServer();
            Platform.runLater(() -> {
                displayProducts(produits);
                setStatus(produits.size() + " produit(s) charge(s)");
            });
        }).start();
    }

    private void displayProducts(List<Produit> produits) {
        productsTable.getItems().setAll(produits);
    }

    @SuppressWarnings("unchecked")
    private List<Produit> fetchProductsFromServer() {
        try {
            Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_ALL_PRODUCTS", null);
            if (response.isSuccess()) {
                return (List<Produit>) response.getData();
            }
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}

