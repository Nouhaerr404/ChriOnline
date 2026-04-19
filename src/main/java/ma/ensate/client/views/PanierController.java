package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.client.utils.NotificationUtils;
import ma.ensate.models.Commande;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.CreerCommandeRequest;
import ma.ensate.protocol.dto.LigneCommandeDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PanierController {

    @FXML private VBox cartItemsContainer;
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;
    @FXML private Label profileInitialLabel;

    @FXML
    public void initialize() {
        chargerPanier();
        if (profileInitialLabel != null) {
            profileInitialLabel.setText(buildUserInitial());
        }
    }

    private void chargerPanier() {
        int clientId = SessionManager.getInstance().getUserId();
        String token = SessionManager.getInstance().getToken();

        new Thread(() -> {
            try {
                Response r = ClientTCP.getInstance().envoyerRequete(
                        new Request("AFFICHER_PANIER", String.valueOf(clientId), token));
                
                Platform.runLater(() -> {
                    if (r != null && r.isSuccess()) {
                        afficherItems((String) r.getData());
                    } else {
                        NotificationUtils.showToast((Stage) totalLabel.getScene().getWindow(), "Erreur chargement panier");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void afficherItems(String data) {
        cartItemsContainer.getChildren().clear();
        if (data == null || data.isBlank()) {
            updateLabels(0.0);
            Label emptyLabel = new Label("Votre panier est vide.");
            emptyLabel.getStyleClass().add("empty-cart-label");
            cartItemsContainer.getChildren().add(emptyLabel);
            return;
        }

        double total = 0.0;
        for (String row : data.split("\n")) {
            if (row.startsWith("TOTAL:")) {
                try { total = Double.parseDouble(row.substring(6).trim().replace(",", ".")); }
                catch (NumberFormatException ignored) {}
                continue;
            }
            
            String[] p = row.split("\\|");
            if (p.length < 5) continue;

            try {
                int id = Integer.parseInt(p[0].trim());
                String nom = p[1].trim();
                double prix = Double.parseDouble(p[2].trim().replace(",", "."));
                int qte = Integer.parseInt(p[3].trim());
                double sub = Double.parseDouble(p[4].trim().replace(",", "."));

                cartItemsContainer.getChildren().add(createCartItemCard(id, nom, prix, qte, sub));
            } catch (Exception ignored) {}
        }
        updateLabels(total);
    }

    private HBox createCartItemCard(int id, String nom, double prix, int qte, double sub) {
        HBox card = new HBox(26);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("cart-item-card");

        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("cart-thumb");
        Label thumbLabel = new Label(buildThumbGlyph(nom));
        thumbLabel.getStyleClass().add("cart-thumb-icon");
        imgBox.getChildren().add(thumbLabel);

        VBox details = new VBox(12);
        details.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(nom);
        nameLabel.getStyleClass().add("cart-item-title");

        Label descriptionLabel = new Label("Architecture premium, finition soignée et intégration parfaite dans votre panier.");
        descriptionLabel.getStyleClass().add("cart-item-description");
        descriptionLabel.setWrapText(true);

        HBox metaRow = new HBox(18);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label refLabel = new Label("RÉF: PRD-" + id);
        refLabel.getStyleClass().add("cart-meta");
        Label stockLabel = new Label("EN STOCK");
        stockLabel.getStyleClass().add("stock-pill");
        metaRow.getChildren().addAll(refLabel, stockLabel);

        details.getChildren().addAll(nameLabel, descriptionLabel, metaRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label subLabel = new Label(String.format("%.2f MAD", sub));
        subLabel.getStyleClass().add("cart-price");

        HBox qteBox = new HBox(14);
        qteBox.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("quantity-btn");
        Label qteLabel = new Label(String.valueOf(qte));
        qteLabel.getStyleClass().add("quantity-label");
        qteLabel.setMinWidth(44);
        qteLabel.setAlignment(Pos.CENTER);
        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("quantity-btn");

        minusBtn.setOnAction(e -> modifierQuantite(id, qte - 1));
        plusBtn.setOnAction(e -> modifierQuantite(id, qte + 1));

        qteBox.getStyleClass().add("quantity-box");
        qteBox.setPadding(new javafx.geometry.Insets(10, 18, 10, 18));
        qteBox.getChildren().addAll(minusBtn, qteLabel, plusBtn);

        VBox controls = new VBox(16);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.getChildren().addAll(subLabel, qteBox);

        Button removeBtn = new Button("RETIRER");
        removeBtn.getStyleClass().add("remove-btn");
        removeBtn.setOnAction(e -> supprimerItem(id));

        card.getChildren().addAll(imgBox, details, spacer, controls, removeBtn);
        return card;
    }

    private String buildThumbGlyph(String nom) {
        if (nom == null || nom.isBlank()) {
            return "◈";
        }

        String normalized = nom.toLowerCase();
        if (normalized.contains("camera") || normalized.contains("vision") || normalized.contains("photo")) {
            return "◉";
        }
        if (normalized.contains("audio") || normalized.contains("head") || normalized.contains("son")) {
            return "◌";
        }
        if (normalized.contains("pc") || normalized.contains("obsidian") || normalized.contains("core")) {
            return "▣";
        }
        return nom.substring(0, 1).toUpperCase();
    }

    private void modifierQuantite(int produitId, int nouvelleQte) {
        if (nouvelleQte <= 0) {
            supprimerItem(produitId);
            return;
        }

        int clientId = SessionManager.getInstance().getUserId();
        String token = SessionManager.getInstance().getToken();

        new Thread(() -> {
            try {
                ClientTCP.getInstance().envoyerRequete(
                        new Request("MODIFIER_QUANTITE_PANIER", clientId + "," + produitId + "," + nouvelleQte, token));
                Platform.runLater(this::chargerPanier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void supprimerItem(int produitId) {
        int clientId = SessionManager.getInstance().getUserId();
        String token = SessionManager.getInstance().getToken();

        new Thread(() -> {
            try {
                ClientTCP.getInstance().envoyerRequete(
                        new Request("SUPPRIMER_DU_PANIER", clientId + "," + produitId, token));
                Platform.runLater(this::chargerPanier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateLabels(double total) {
        subtotalLabel.setText(String.format("%.2f MAD", total));
        totalLabel.setText(String.format("%.2f MAD", total));
    }

    @FXML
    private void retourBoutique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) totalLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToOrders() {
        new HistoriqueView(
            (Stage) totalLabel.getScene().getWindow(),
            ClientTCP.getInstance(),
            SessionManager.getInstance().getUserId(),
            SessionManager.getInstance().getToken()
        ).afficher();
    }

    @FXML
    private void procederAuPaiement() {
        int clientId = SessionManager.getInstance().getUserId();
        String token = SessionManager.getInstance().getToken();
        Stage stage = (Stage) totalLabel.getScene().getWindow();

        new Thread(() -> {
            try {
                // 1. Récupérer le panier (Format String du serveur)
                Response cartResp = ClientTCP.getInstance().envoyerRequete(
                    new Request("AFFICHER_PANIER", String.valueOf(clientId), token)
                );

                if (!cartResp.isSuccess() || cartResp.getData() == null) {
                    Platform.runLater(() -> NotificationUtils.showToast(stage, "Panier vide ou erreur serveur."));
                    return;
                }

                String data = (String) cartResp.getData();
                List<LigneCommandeDTO> lignes = new ArrayList<>();

                // Parsing de la réponse brute du serveur
                for (String row : data.split("\n")) {
                    if (row.startsWith("TOTAL:") || row.isBlank()) continue;
                    String[] p = row.split("\\|");
                    if (p.length >= 4) {
                        try {
                            int prodId = Integer.parseInt(p[0].trim());
                            int qte = Integer.parseInt(p[3].trim());
                            lignes.add(new LigneCommandeDTO(prodId, qte));
                        } catch (Exception ignored) {}
                    }
                }

                if (lignes.isEmpty()) {
                    Platform.runLater(() -> NotificationUtils.showToast(stage, "Votre panier est vide."));
                    return;
                }

                // 2. Création de la commande sur le serveur
                CreerCommandeRequest req = new CreerCommandeRequest(clientId, lignes);
                Response orderResp = ClientTCP.getInstance().envoyerRequete(
                    new Request("CREER_COMMANDE", req, token)
                );

                Platform.runLater(() -> {
                    if (orderResp.isSuccess() && orderResp.getData() instanceof Commande) {
                        Commande realCommande = (Commande) orderResp.getData();
                        new PaiementView(stage, ClientTCP.getInstance(), clientId, token, realCommande).afficher();
                    } else {
                        NotificationUtils.showToast(stage, "Erreur serveur : " + orderResp.getMessage());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> NotificationUtils.showToast(stage, "Erreur réseau : " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void goToProfil() {
        System.out.println("[Navigation] Panier -> Profil");
        navigateToProfil();
    }

    @FXML
    private void goToProfilMouse(javafx.scene.input.MouseEvent event) {
        navigateToProfil();
    }

    private void navigateToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/profil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) totalLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Mon Profil");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildUserInitial() {
        String nom = SessionManager.getInstance().getNomUtilisateur();
        return (nom != null && !nom.isBlank()) ? nom.trim().substring(0, 1).toUpperCase() : "U";
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) totalLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
