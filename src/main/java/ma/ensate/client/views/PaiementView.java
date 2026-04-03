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
            lblSub.setText("Commande #" + commande.getId().substring(0, 8).toUpperCase());
            valT.setText(String.format("%.2f MAD", commande.getPrixAPayer()));

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
                montrerAlerte("Données manquantes", "Le numéro de carte doit contenir 16 chiffres.");
                return;
            }

            // CVV : exactement 3 chiffres
            if (txtCvv.getText().length() != 3) {
                montrerAlerte("Données manquantes", "Le CVV doit contenir exactement 3 chiffres.");
                return;
            }

            // Date d'expiration : format MM/AA
            if (!txtExp.getText().matches("\\d{2}/\\d{2}")) {
                montrerAlerte("Données manquantes", "La date d'expiration doit être au format MM/AA.");
                return;
            }

            int mois = Integer.parseInt(txtExp.getText().substring(0, 2));
            if (mois < 1 || mois > 12) {
                montrerAlerte("Date invalide", "Le mois doit être compris entre 01 et 12.");
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
                        montrerMessageSucces();
                    } else {
                        montrerAlerte("Échec", res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> montrerAlerte("Erreur réseau", "Connexion perdue."));
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

    private void montrerAlerte(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void montrerMessageSucces() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paiement Réussi");
        alert.setHeaderText("Félicitations !");
        alert.setContentText("Votre paiement a été accepté. Votre commande est en préparation.");
        alert.showAndWait();
        
        retourBoutique();
    }

    @FXML
    private void retourBoutique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handlers pour les effets de survol du bouton
    @FXML private void btnPayHover() { btnPay.setOpacity(0.85); }
    @FXML private void btnPayExit() { btnPay.setOpacity(1.0); }
}
