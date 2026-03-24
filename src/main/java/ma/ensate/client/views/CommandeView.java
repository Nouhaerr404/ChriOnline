package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Commande;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.CreerCommandeRequest;
import ma.ensate.protocol.dto.LigneCommandeDTO;

import java.util.ArrayList;
import java.util.List;

public class CommandeView {

    // --- Design System IT ---
    private static final String NAVY       = "#0F172A"; // Navy sombre
    private static final String ACCENT     = "#3B82F6"; // Bleu IT
    private static final String TEXT_MAIN  = "#1E293B";
    private static final String TEXT_SUB   = "#64748B";

    private final Stage stage;          // La fenêtre principale
    private final ClientTCP clientTCP;   // Pour communiquer avec le serveur
    private final int clientId;          // ID du client connecté
    private final String token;          // Token d'authentification
    private final double total;           // Montant total de la commande
    private final List<PanierView.LigneTableau> articles;  // Liste des articles du panier
    
    @FXML private StackPane rootPane;
    @FXML private VBox itemBox;
    @FXML private Label lblTotalPrix;
    @FXML private Button btnConfirmer;

    public CommandeView(Stage stage, ClientTCP clientTCP, int clientId, String token, double total, List<PanierView.LigneTableau> articles) {
        this.stage = stage;
        this.clientTCP = clientTCP;
        this.clientId = clientId;
        this.token = token;
        this.total = total;
        this.articles = articles;
    }

    public void afficher() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/commande.fxml"));
            loader.setController(this);
            Parent root = loader.load(); //maintenant toute la hiérarchie de l'interface
            
            // On parcourt la liste des articles présents dans le panier pour créer une ligne graphique pour chacun.
            for (PanierView.LigneTableau art : articles) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(5, 0, 5, 0));
                
                VBox nameBox = new VBox(2);
                Label name = new Label(art.getNomProduit());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + TEXT_MAIN + ";");
                Label qty = new Label("Quantité : " + art.getQuantite());
                qty.setStyle("-fx-text-fill: " + TEXT_SUB + "; -fx-font-size: 13px;");
                nameBox.getChildren().addAll(name, qty);

                Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
                Label price = new Label(String.format("%.2f MAD", art.getSubtotal()));
                price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

                row.getChildren().addAll(nameBox, s, price);
                itemBox.getChildren().add(row);
            }

            lblTotalPrix.setText(String.format("%.2f MAD", total));

            Scene scene = new Scene(root, 1000, 700);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retourPanier() {
        new PanierView(stage, clientTCP, clientId, token).afficher();
    }

    @FXML
    private void ouvrirHistorique() {
        new HistoriqueView(stage, clientTCP, clientId, token).afficher();
    }

    @FXML
    private void btnHover() {
        btnConfirmer.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
    }

    @FXML
    private void btnExit() {
        btnConfirmer.setStyle("-fx-background-color: " + NAVY + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-letter-spacing: 1px; -fx-background-radius: 10; -fx-cursor: hand;");
    }

    @FXML
    private void envoyerCommande() {
        if ("mock-token".equals(token)) {
            Commande mockCmd = new Commande();
            mockCmd.setId("MOCK-ORD-" + (int)(Math.random()*9000));
            mockCmd.setPrixAPayer(total);
            new PaiementView(stage, clientTCP, clientId, token, mockCmd).afficher();
            return;
        }

        showLoadingOverlay();
        // Convertit les articles du panier en DTO pour l'envoi au serveur
        List<LigneCommandeDTO> lignesDTO = new ArrayList<>();
        for (PanierView.LigneTableau art : articles) {
            lignesDTO.add(new LigneCommandeDTO(art.getProduitId(), art.getQuantite()));
        }

        new Thread(() -> {
            try {
                // ENVOI DE LA REQUÊTE AU SERVEUR
                Response resp = clientTCP.envoyerRequete(new Request("CREER_COMMANDE", new CreerCommandeRequest(clientId, lignesDTO), token));
                Platform.runLater(() -> {             // Traitement de la réponse (sur le thread JavaFX)
                    hideLoadingOverlay(); // Cache "Chargement..."
                    if (resp.isSuccess()) {
                        // SUCCÈS : La commande est créée, on passe au paiement
                        new PaiementView(stage, clientTCP, clientId, token, (Commande) resp.getData()).afficher();
                    } else {
                        showModernPopup("Erreur", resp.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideLoadingOverlay();
                    showModernPopup("Erreur Réseau", "Impossible de contacter le centre de commande.", true);
                });
            }
        }).start();
    }


    public void showModernPopup(String title, String message, boolean isError) {
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        overlay.setAlignment(Pos.CENTER);

        VBox popup = new VBox(20);
        popup.setMaxSize(400, 200);
        popup.setPadding(new Insets(30));
        popup.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        popup.setEffect(new DropShadow(20, Color.BLACK));
        popup.setAlignment(Pos.CENTER);

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + (isError ? "#EF4444" : ACCENT) + ";");
        
        Label m = new Label(message);
        m.setWrapText(true);
        m.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-text-alignment: center;");

        Button btn = new Button("COMPRIS");
        btn.setStyle("-fx-background-color: " + NAVY + "; -fx-text-fill: white; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setOnAction(e -> rootPane.getChildren().remove(overlay));

        popup.getChildren().addAll(t, m, btn);
        overlay.getChildren().add(popup);
        rootPane.getChildren().add(overlay);
    }

    
    //Cette méthode construit visuellement l'indicateur de patience 
    private void showLoadingOverlay() {
        VBox loading = new VBox(15, new ProgressIndicator(), new Label("Traitement de votre commande..."));
        loading.setId("loader");
        loading.setStyle("-fx-background-color: rgba(255,255,255,0.8);");
        loading.setAlignment(Pos.CENTER);
        rootPane.getChildren().add(loading);
    }
    //nettoie l'interface une fois le traitement terminé
    private void hideLoadingOverlay() {
        rootPane.getChildren().removeIf(node -> "loader".equals(node.getId()));
    }
}

