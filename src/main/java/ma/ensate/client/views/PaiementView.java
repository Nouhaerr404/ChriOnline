package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

public class PaiementView {

    // --- Design System IT ---
    private static final String NAVY       = "#0F172A";
    private static final String ACCENT     = "#3B82F6"; 
    private static final String SUCCESS    = "#10B981";

    private final Stage stage;
    private final ClientTCP clientTCP;
    private final int clientId;
    private final String token;
    private final Commande commande;
    
    @FXML private StackPane rootPane;
    @FXML private Label lblSub;
    @FXML private Label valT;
    @FXML private RadioButton rbCard;
    @FXML private RadioButton rbCash;
    @FXML private VBox cardForm;
    @FXML private TextField txtNum;
    @FXML private TextField txtExp;
    @FXML private TextField txtCvv;
    @FXML private TextField txtHold;
    @FXML private Button btnPay;

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

            lblSub.setText("Commande #" + commande.getId().substring(0, 8).toUpperCase());
            valT.setText(String.format("%.2f MAD", commande.getPrixAPayer()));
            
            cardForm.visibleProperty().bind(rbCard.selectedProperty());
            cardForm.managedProperty().bind(cardForm.visibleProperty());

            stage.setScene(new Scene(root, 1000, 700));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void btnPayHover() {
        btnPay.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
    }

    @FXML
    private void btnPayExit() {
        btnPay.setStyle("-fx-background-color: " + SUCCESS + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
    }

    @FXML
    private void handlePayment() {
        boolean valid = true;
        String cardNum = txtNum.getText() != null ? txtNum.getText().replace(" ", "") : "";
        if (rbCard.isSelected()) {
            if (cardNum.length() < 12) valid = false;
            if (txtExp.getText() == null || txtExp.getText().isEmpty()) valid = false;
            if (txtCvv.getText() == null || txtCvv.getText().length() < 3) valid = false;
        }
        
        if (!valid) {
            showModernPopup("Informations Incomplètes", "Veuillez remplir tous les champs de la carte.", true);
        } else {
            // On prend les 4 derniers chiffres pour l'envoyer au serveur
            String last4 = cardNum.length() >= 4 ? cardNum.substring(cardNum.length() - 4) : "0000";
            processPayment(rbCard.isSelected(), last4);
        }
    }



    private void processPayment(boolean isCard, String last4) {
        if ("mock-token".equals(token)) { showSuccess(); return; }

        if (isCard && (last4.length() != 4 || !last4.matches("\\d+"))) {  //"\\d+" : "une suite de un ou plusieurs chiffres".
            showModernPopup("Erreur Carte", "Veuillez entrer les 4 chiffres de votre carte.", true);
            return;
        }

        showLoading();
        MethodePaiement methode = isCard ? MethodePaiement.CARTE_BANCAIRE : MethodePaiement.ALIVRAISON;
        PaiementRequest req = new PaiementRequest(commande.getId(), methode.name(), isCard ? last4 : null);

        //"On utilise un Thread séparé pour effectuer la requête réseau en arrière-plan.
        // Cela évite de bloquer l'UI Thread (le fil principal), ce qui rendrait l'application non-réactive pendant l'attente de la réponse du serveur.
        // Une fois la réponse reçue, on utilise Platform.runLater() pour revenir sur le fil principal et mettre à jour l'affichage en toute sécurité."
        new Thread(() -> {
            try {
                Response resp = clientTCP.envoyerRequete(new Request("EFFECTUER_PAIEMENT", req, token));
                Platform.runLater(() -> {
                    hideLoading();
                    if (resp.isSuccess()) {
                        new Thread(() -> {
                            try { clientTCP.envoyerRequete(new Request("VIDER_PANIER", String.valueOf(clientId), token)); } 
                            catch (Exception ignored) {}
                        }).start();
                        showSuccess();
                    } else {
                        showModernPopup("Échec", resp.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> { hideLoading(); showModernPopup("Erreur", "Problème de connexion.", true); });
            }
        }).start();
    }

    private void showSuccess() {
        VBox overlay = new VBox(25);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: white;");

        Label icon = new Label("✔");
        icon.setStyle("-fx-font-size: 80px; -fx-text-fill: " + SUCCESS + "; -fx-font-weight: bold;");
        
        Label t = new Label("Paiement Confirmé !");
        t.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");
        
        Label m = new Label("Votre commande IT est en cours de préparation dans nos entrepôts.");
        m.setStyle("-fx-text-fill: #64748B; -fx-font-size: 16px;");

        Button btnHome = new Button("RETOUR À LA BOUTIQUE");
        btnHome.setStyle("-fx-background-color: " + NAVY + "; -fx-text-fill: white; -fx-padding: 15 40; -fx-background-radius: 30; -fx-font-weight: bold; -fx-cursor: hand;");
        btnHome.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
                Parent root = loader.load();
                stage.getScene().setRoot(root);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        overlay.getChildren().addAll(icon, t, m, btnHome);
        rootPane.getChildren().add(overlay);
    }

    private void showModernPopup(String title, String message, boolean isError) {
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7);");
        overlay.setAlignment(Pos.CENTER);
        VBox p = new VBox(15, new Label(title) {{ setStyle("-fx-font-weight:bold; -fx-font-size:18px; -fx-text-fill:"+(isError?"#EF4444":ACCENT)); }}, new Label(message), new Button("OK") {{ setOnAction(e -> rootPane.getChildren().remove(overlay)); }});
        p.setStyle("-fx-background-color:white; -fx-padding:30; -fx-background-radius:15;");
        p.setAlignment(Pos.CENTER);
        overlay.getChildren().add(p);
        rootPane.getChildren().add(overlay);
    }

    private void showLoading() {
        VBox l = new VBox(new ProgressIndicator()); l.setId("loader"); l.setAlignment(Pos.CENTER); l.setStyle("-fx-background-color:rgba(255,255,255,0.7)");
        rootPane.getChildren().add(l);
    }
    private void hideLoading() { rootPane.getChildren().removeIf(n -> "loader".equals(n.getId())); }
}
