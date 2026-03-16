package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Commande;
import ma.ensate.models.MethodePaiement;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.PaiementRequest;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class PaiementView {

    // --- Design System IT ---
    private static final String NAVY       = "#0F172A";
    private static final String ACCENT     = "#3B82F6"; 
    private static final String BG_LIGHT   = "#F8FAFC";
    private static final String SUCCESS    = "#10B981";

    private final Stage stage;
    private final ClientTCP clientTCP;
    private final int clientId;
    private final String token;
    private final Commande commande;
    private StackPane rootPane;

    public PaiementView(Stage stage, ClientTCP clientTCP, int clientId, String token, Commande commande) {
        this.stage = stage;
        this.clientTCP = clientTCP;
        this.clientId = clientId;
        this.token = token;
        this.commande = commande;
    }

    public void afficher() {
        rootPane = new StackPane();
        VBox layout = new VBox(30);
        layout.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-padding: 0;");
        layout.setAlignment(Pos.TOP_CENTER);

        // Header
        layout.getChildren().add(creerSimpleHeader());

        VBox card = new VBox(25);
        card.setMaxWidth(500);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 40;");
        card.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.1)));
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Paiement Sécurisé");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");
        
        Label sub = new Label("Commande #" + commande.getId().substring(0, 8).toUpperCase());
        sub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14px;");

        HBox totalRow = new HBox(10);
        totalRow.setAlignment(Pos.CENTER);
        Label lblT = new Label("Montant :");
        Label valT = new Label(String.format("%.2f MAD", commande.getPrixAPayer()));
        valT.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + ACCENT + ";");
        totalRow.getChildren().addAll(lblT, valT);

        // Options de paiement stylées
        VBox methods = new VBox(15);
        methods.setPadding(new Insets(10, 0, 10, 0));
        
        ToggleGroup group = new ToggleGroup();
        RadioButton rbCard = new RadioButton("Carte Bancaire (VISA/Mastercard)");
        rbCard.setToggleGroup(group);
        rbCard.setSelected(true);
        rbCard.setStyle("-fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        // --- FORMULAIRE CARTE DÉTAILLÉ ---
        VBox cardForm = new VBox(15);
        cardForm.setPadding(new Insets(10, 0, 0, 25));
        
        // Numéro de carte
        VBox numBox = new VBox(5);
        Label lblNum = new Label("Numéro de carte:");
        lblNum.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        TextField txtNum = new TextField();
        txtNum.setPromptText("xxxx xxxx xxxx xxxx");
        txtNum.setPrefHeight(45);
        txtNum.setStyle("-fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-font-family: 'Monospaced';");
        numBox.getChildren().addAll(lblNum, txtNum);

        // Exp + CVV
        HBox rowExpCvv = new HBox(15);
        VBox expBox = new VBox(5);
        Label lblExp = new Label("Date d'expiration:");
        lblExp.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        TextField txtExp = new TextField();
        txtExp.setPromptText("MM / AA");
        txtExp.setPrefHeight(45);
        txtExp.setStyle("-fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
        expBox.getChildren().addAll(lblExp, txtExp);

        VBox cvvBox = new VBox(5);
        Label lblCvv = new Label("CVV:");
        lblCvv.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        TextField txtCvv = new TextField();
        txtCvv.setPromptText("123");
        txtCvv.setPrefHeight(45);
        txtCvv.setStyle("-fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
        cvvBox.getChildren().addAll(lblCvv, txtCvv);
        rowExpCvv.getChildren().addAll(expBox, cvvBox);

        // Titulaire
        VBox holderBox = new VBox(5);
        Label lblHold = new Label("Titulaire de la carte:");
        lblHold.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        TextField txtHold = new TextField();
        txtHold.setPromptText("PRÉNOM NOM");
        txtHold.setPrefHeight(45);
        txtHold.setStyle("-fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
        holderBox.getChildren().addAll(lblHold, txtHold);

        cardForm.getChildren().addAll(numBox, rowExpCvv, holderBox);
        cardForm.visibleProperty().bind(rbCard.selectedProperty());
        cardForm.managedProperty().bind(cardForm.visibleProperty());

        RadioButton rbCash = new RadioButton("Paiement à la Livraison");
        rbCash.setToggleGroup(group);
        rbCash.setStyle("-fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        methods.getChildren().addAll(rbCard, cardForm, rbCash);

        Button btnPay = new Button("CONFIRMER LE RÈGLEMENT");
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setPrefHeight(50);
        btnPay.setStyle("-fx-background-color: " + SUCCESS + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        btnPay.setOnMouseEntered(e -> btnPay.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;"));
        btnPay.setOnMouseExited(e -> btnPay.setStyle("-fx-background-color: " + SUCCESS + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;"));
        btnPay.setOnAction(e -> {
            boolean valid = true;
            String cardNum = txtNum.getText().replace(" ", "");
            if (rbCard.isSelected()) {
                if (cardNum.length() < 12) valid = false;
                if (txtExp.getText().isEmpty()) valid = false;
                if (txtCvv.getText().length() < 3) valid = false;
            }
            
            if (!valid) {
                showModernPopup("Informations Incomplètes", "Veuillez remplir tous les champs de la carte.", true);
            } else {
                // On prend les 4 derniers chiffres pour l'envoyer au serveur
                String last4 = cardNum.length() >= 4 ? cardNum.substring(cardNum.length() - 4) : "0000";
                processPayment(rbCard.isSelected(), last4);
            }
        });

        card.getChildren().addAll(title, sub, totalRow, new Separator(), methods, btnPay);
        layout.getChildren().add(card);

        rootPane.getChildren().add(layout);
        stage.setScene(new Scene(rootPane, 1000, 700));
        stage.show();
    }

    private HBox creerSimpleHeader() {
        HBox h = new HBox(logoBox());
        h.setPadding(new Insets(20, 50, 20, 50));
        h.setStyle("-fx-background-color: " + NAVY + ";");
        return h;
    }

    private VBox logoBox() {
        Label logo = new Label("ChriOnline");
        logo.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label tag = new Label("CHECKOUT");
        tag.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        return new VBox(-3, logo, tag);
    }

    private void processPayment(boolean isCard, String last4) {
        if ("mock-token".equals(token)) { showSuccess(); return; }

        if (isCard && (last4.length() != 4 || !last4.matches("\\d+"))) {
            showModernPopup("Erreur Carte", "Veuillez entrer les 4 chiffres de votre carte.", true);
            return;
        }

        showLoading();
        MethodePaiement methode = isCard ? MethodePaiement.CARTE_BANCAIRE : MethodePaiement.ALIVRAISON;
        PaiementRequest req = new PaiementRequest(commande.getId(), methode.name(), isCard ? last4 : null);

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
