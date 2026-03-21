package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.ProduitAdminRequest;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class ProduitFormView {

    @FXML private Label titleLabel;
    @FXML private TextField nomField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private ComboBox<Categorie> categorieComboBox;
    @FXML private Label imageNameLabel;
    @FXML private Label statusLabel;

    private Produit produitToEdit;
    private byte[] selectedImageBytes;
    private String selectedImageFileName;
    private String currentImageUrl;

    @FXML
    public void initialize() {
        loadCategories();
    }

    public void setProduitToEdit(Produit produit) {
        this.produitToEdit = produit;
        this.currentImageUrl = produit.getImageUrl();

        titleLabel.setText("Modifier le produit");
        nomField.setText(produit.getNom());
        descriptionArea.setText(produit.getDescription());
        prixField.setText(String.valueOf(produit.getPrix()));
        stockField.setText(String.valueOf(produit.getStock()));

        if (produit.getImageUrl() != null && !produit.getImageUrl().isBlank()) {
            imageNameLabel.setText("Image actuelle: " + produit.getImageUrl());
        }

        if (produit.getCategorie() != null) {
            categorieComboBox.getSelectionModel().select(
                    categorieComboBox.getItems().stream()
                            .filter(cat -> cat.getId() == produit.getCategorie().getId())
                            .findFirst()
                            .orElse(null)
            );
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image produit");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) nomField.getScene().getWindow();
        File selected = fileChooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }

        try {
            selectedImageBytes = Files.readAllBytes(selected.toPath());
            selectedImageFileName = selected.getName();
            imageNameLabel.setText("Nouvelle image: " + selectedImageFileName);
            setStatus("Image selectionnee");
        } catch (Exception e) {
            setStatus("Impossible de lire l'image: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        ProduitAdminRequest request = buildRequest();
        if (request == null) {
            return;
        }

        String action = produitToEdit == null ? "CREATE_PRODUCT" : "UPDATE_PRODUCT";
        setStatus("Enregistrement en cours...");

        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee(action, request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        goBackToAdminList();
                    } else {
                        setStatus("Echec: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Erreur reseau: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        goBackToAdminList();
    }

    @FXML
    private void handleBackTop() {
        goBackToAdminList();
    }

    private ProduitAdminRequest buildRequest() {
        String nom = nomField.getText() != null ? nomField.getText().trim() : "";
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        String prixText = prixField.getText() != null ? prixField.getText().trim() : "";
        String stockText = stockField.getText() != null ? stockField.getText().trim() : "";
        Categorie categorie = categorieComboBox.getSelectionModel().getSelectedItem();

        if (nom.isBlank()) {
            setStatus("Le nom du produit est obligatoire");
            return null;
        }
        if (categorie == null || categorie.getId() <= 0) {
            setStatus("Selectionnez une categorie valide");
            return null;
        }

        double prix;
        int stock;
        try {
            prix = Double.parseDouble(prixText);
            stock = Integer.parseInt(stockText);
        } catch (NumberFormatException e) {
            setStatus("Prix/stock invalides");
            return null;
        }

        ProduitAdminRequest req = new ProduitAdminRequest();
        req.setNom(nom);
        req.setDescription(description);
        req.setPrix(prix);
        req.setStock(stock);
        req.setCategorieId(categorie.getId());
        req.setImageUrl(currentImageUrl);

        if (produitToEdit != null) {
            req.setId(produitToEdit.getId());
        }

        if (selectedImageBytes != null && selectedImageBytes.length > 0) {
            req.setImageBytes(selectedImageBytes);
            req.setImageFileName(selectedImageFileName);
        }

        return req;
    }

    private void loadCategories() {
        new Thread(() -> {
            List<Categorie> categories = fetchCategoriesFromServer();
            Platform.runLater(() -> {
                categorieComboBox.getItems().setAll(categories);

                if (produitToEdit != null && produitToEdit.getCategorie() != null) {
                    categorieComboBox.getSelectionModel().select(
                            categories.stream()
                                    .filter(c -> c.getId() == produitToEdit.getCategorie().getId())
                                    .findFirst()
                                    .orElse(null)
                    );
                } else if (!categories.isEmpty()) {
                    categorieComboBox.getSelectionModel().selectFirst();
                }
            });
        }).start();
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

    private void goBackToAdminList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Dashboard Admin Produits");
        } catch (Exception e) {
            setStatus("Erreur navigation: " + e.getMessage());
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}
