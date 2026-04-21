package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Commande;
import ma.ensate.models.MethodePaiement;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.PaiementRequest;
import javafx.scene.paint.Color;

public class PaiementView {

    @FXML private StackPane rootPane;
    @FXML private Label lblSub;
    @FXML private Label valT;
    @FXML private RadioButton rbCard;
    @FXML private RadioButton rbCash;
    @FXML private VBox cardForm;
    @FXML private TextField txtNum, txtExp, txtCvv, txtHold;
    @FXML private Button btnPay;
    @FXML private VBox livraisonForm;
    @FXML private RadioButton rbLivraisonCash;
    @FXML private RadioButton rbLivraisonCard;
    @FXML private Label profileInitialLabel;

    private final Stage stage;
    private final ClientTCP clientTCP;
    private final int clientId;
    private final String token;
    private final Commande commande;

    public PaiementView(Stage stage, ClientTCP clientTCP, int clientId, String token, Commande commande) {
        this.stage = stage;
        this.clientTCP = clientTCP;
        this.clientId = clientId;
        this.token = token;
        this.commande = commande;
    }

    public void afficher() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/paiement.fxml"));
            loader.setController(this);
            Parent root = loader.load();

            // Initialisation des données
            String safeId = (commande.getId() != null) ? commande.getId() : "NR";
            lblSub.setText("Commande #" + safeId.substring(0, Math.min(safeId.length(), 10)).toUpperCase());
            valT.setText(String.format("%.2f MAD", commande.getPrixAPayer()));

            if (profileInitialLabel != null) {
                profileInitialLabel.setText(buildUserInitial());
            }

            // Gestion de l'affichage du formulaire carte
            cardForm.setVisible(true);


            rbCard.setOnAction(e -> {
                cardForm.setVisible(true);
                cardForm.setManaged(true);
                livraisonForm.setVisible(false);
                livraisonForm.setManaged(false);
            });
            rbCash.setOnAction(e -> {
                cardForm.setVisible(false);
                cardForm.setManaged(false);
                livraisonForm.setVisible(true);
                livraisonForm.setManaged(true);
            });

            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePayment() {
        if (rbCard.isSelected()) {
            String num = txtNum.getText().replace(" ", "");

            // Numéro de carte : exactement 16 chiffres
            if (num.length() != 16) {
                showAlert("Données manquantes", "Le numéro de carte doit contenir 16 chiffres.", false);
                return;
            }

            // CVV : exactement 3 chiffres
            if (txtCvv.getText().length() != 3) {
                showAlert("Données manquantes", "Le CVV doit contenir exactement 3 chiffres.", false);
                return;
            }

            // Date d'expiration : format MM/AA
            if (!txtExp.getText().matches("\\d{2}/\\d{2}")) {
                showAlert("Données manquantes", "La date d'expiration doit être au format MM/AA.", false);
                return;
            }

            int mois = Integer.parseInt(txtExp.getText().substring(0, 2));
            if (mois < 1 || mois > 12) {
                showAlert("Date invalide", "Le mois doit être compris entre 01 et 12.", false);
                return;
            }
        }

        // On lance le paiement
        String last4 = "0000";
        if (rbCard.isSelected()) {
            String num = txtNum.getText().replace(" ", "");
            last4 = num.substring(num.length() - 4);
        }

        traiterPaiement(last4);
    }

    private void traiterPaiement(String last4) {
     
        String methode;
        if (rbCard.isSelected()) {
            methode = "CARTE_BANCAIRE";
        } else if (rbLivraisonCard.isSelected()) {
            methode = "ALIVRAISON_CARTE";
        } else {
            methode = "ALIVRAISON";
        }

        PaiementRequest dt = new PaiementRequest(commande.getId(), methode, last4);

        new Thread(() -> {
            try {
                Response res = clientTCP.envoyerRequete(new Request("EFFECTUER_PAIEMENT", dt, token));

                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        viderPanier();
                        showAlert("Paiement Réussi", "Félicitations ! Votre paiement a été accepté. Votre commande est en préparation.", true);
                    } else {
                        showAlert("Échec du paiement", res.getMessage(), false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Erreur réseau", "Impossible de joindre le serveur de paiement.", false));
            }
        }).start();
    }

    private void viderPanier() {
        new Thread(() -> {
            try {
                clientTCP.envoyerRequete(new Request("VIDER_PANIER", String.valueOf(clientId), token));
            } catch (Exception ignored) {}
        }).start();
    }

    private void showAlert(String titre, String msg, boolean isSuccess) {
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color: rgba(7, 18, 36, 0.85);");
        overlay.setAlignment(javafx.geometry.Pos.CENTER);

        VBox popup = new VBox(20);
        popup.setMaxWidth(450);
        popup.setPadding(new javafx.geometry.Insets(35));
        popup.getStyleClass().add("summary-card");
        popup.setStyle("-fx-background-color: #0d1b31; -fx-background-radius: 20; -fx-border-color: " + (isSuccess ? "#5cff90" : "#ff7ea8") + "; -fx-border-width: 1.5;");
        popup.setEffect(new javafx.scene.effect.DropShadow(30, Color.BLACK));

        Label t = new Label(titre.toUpperCase());
        t.getStyleClass().add("section-title-accent");
        t.setStyle("-fx-text-fill: " + (isSuccess ? "#5cff90" : "#ff7ea8") + "; -fx-font-size: 18px;");

        Label m = new Label(msg);
        m.getStyleClass().add("hero-subtitle");
        m.setWrapText(true);
        m.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        m.setStyle("-fx-font-size: 14px;");

        Button btn = new Button(isSuccess ? "RETOUR À LA BOUTIQUE" : "RÉESSAYER");
        btn.getStyleClass().add(isSuccess ? "product-action-btn" : "secondary-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(50);
        btn.setOnAction(e -> {
            rootPane.getChildren().remove(overlay);
            if (isSuccess) retourBoutique();
        });

        popup.getChildren().addAll(t, m, btn);
        overlay.getChildren().add(popup);
        rootPane.getChildren().add(overlay);
    }

    private String buildUserInitial() {
        String nom = ma.ensate.client.network.SessionManager.getInstance().getNomUtilisateur();
        return (nom != null && !nom.isBlank()) ? nom.trim().substring(0, 1).toUpperCase() : "U";
    }

    @FXML private void goToProfil() {
        System.out.println("[Navigation] Paiement -> Profil");
        navigateToProfil();
    }
    @FXML private void goToProfilMouse(javafx.scene.input.MouseEvent event) { navigateToProfil(); }

    private void navigateToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/profil.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Mon Profil");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void goToPanier() {
        new PanierView(stage, clientTCP, clientId, token).afficher();
    }

    @FXML private void goToOrders() {
        new HistoriqueView(stage, clientTCP, clientId, token).afficher();
    }

    @FXML private void handleLogout() {
        ma.ensate.client.network.SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleRetour() {
        try {
            Stage stage = (Stage) valT.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/panier.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Mon Panier");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retourBoutique() {
        try {
            Stage stage = (Stage) valT.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Boutique");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handlers pour les effets de survol du bouton
    @FXML private void btnPayHover() { btnPay.setOpacity(0.85); }
    @FXML private void btnPayExit() { btnPay.setOpacity(1.0); }
}
