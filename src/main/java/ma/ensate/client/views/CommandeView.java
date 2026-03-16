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
    private static final String BG_LIGHT   = "#F8FAFC";
    private static final String TEXT_MAIN  = "#1E293B";
    private static final String TEXT_SUB   = "#64748B";

    private final Stage stage;          // La fenêtre principale
    private final ClientTCP clientTCP;   // Pour communiquer avec le serveur
    private final int clientId;          // ID du client connecté
    private final String token;          // Token d'authentification
    private final double total;           // Montant total de la commande
    private final List<PanierView.LigneTableau> articles;  // Liste des articles du panier
    //Le constructeur reçoit toutes ces données depuis la vue du panier (PanierView).
    private StackPane rootPane;

    public CommandeView(Stage stage, ClientTCP clientTCP, int clientId, String token, double total, List<PanierView.LigneTableau> articles) {
        this.stage = stage;
        this.clientTCP = clientTCP;
        this.clientId = clientId;
        this.token = token;
        this.total = total;
        this.articles = articles;
    }

    public void afficher() {
        // Crée l'interface utilisateur
        rootPane = new StackPane();
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        // Header IT
        layout.setTop(creerHeader());

        // Content
        VBox content = new VBox(30);
        content.setPadding(new Insets(40));
        content.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("Finalisation de votre commande");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");
        
        VBox card = new VBox(20);
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 35;");
        card.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.08)));

        // Liste des articles
        VBox itemBox = new VBox(12);
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

        Separator sep = new Separator();
        sep.setPadding(new Insets(10, 0, 10, 0));

        // Total
        HBox totalRow = new HBox();
        Label totL = new Label("Total de la commande");
        totL.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + ";");
        // Crée une région (espace vide) qui va jouer le rôle de "ressort" pour pousser les éléments
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        Label totV = new Label(String.format("%.2f MAD", total));
        totV.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + ACCENT + ";");
        totalRow.getChildren().addAll(totL, s2, totV);

        // Bouton 
        Button btnConfirmer = new Button("PROCÉDER AU PAIEMENT SÉCURISÉ");
        btnConfirmer.setPrefHeight(55);
        btnConfirmer.setMaxWidth(Double.MAX_VALUE);
        btnConfirmer.setStyle("-fx-background-color: " + NAVY + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-letter-spacing: 1px; -fx-background-radius: 10; -fx-cursor: hand;");
        btnConfirmer.setOnMouseEntered(e -> btnConfirmer.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;"));
        btnConfirmer.setOnMouseExited(e -> btnConfirmer.setStyle("-fx-background-color: " + NAVY + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;"));
        btnConfirmer.setOnAction(e -> envoyerCommande());

        card.getChildren().addAll(itemBox, sep, totalRow, btnConfirmer);
        content.getChildren().addAll(titleLabel, card);

        layout.setCenter(new ScrollPane(content) {{ 
            setFitToWidth(true); 
            setStyle("-fx-background-color: transparent; -fx-background: transparent;"); 
        }});

        rootPane.getChildren().add(layout);
        Scene scene = new Scene(rootPane, 1000, 700);
        stage.setScene(scene);
        stage.show();
    }

    private HBox creerHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 50, 20, 50));
        header.setStyle("-fx-background-color: " + NAVY + ";");

        Label logo = new Label("ChriOnline");
        logo.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        
        Label tag = new Label("ELECTRONICS HUB");
        tag.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox logoBox = new VBox(-3, logo, tag);

        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);

        Button btnRetourPanier = new Button("← Retour au panier");
        btnRetourPanier.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-cursor: hand; -fx-font-weight: bold;");
        btnRetourPanier.setOnAction(e -> new PanierView(stage, clientTCP, clientId, token).afficher());

        Button btnHistorique = new Button("Mes Commandes");
        btnHistorique.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-cursor: hand;");
        btnHistorique.setOnAction(e -> new HistoriqueView(stage, clientTCP, clientId, token).afficher());

        header.getChildren().addAll(logoBox, s, btnRetourPanier, btnHistorique);
        return header;
    }

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

    // --- MODERNE UI UTILS ---

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

    private void showLoadingOverlay() {
        VBox loading = new VBox(15, new ProgressIndicator(), new Label("Traitement de votre commande..."));
        loading.setId("loader");
        loading.setStyle("-fx-background-color: rgba(255,255,255,0.8);");
        loading.setAlignment(Pos.CENTER);
        rootPane.getChildren().add(loading);
    }

    private void hideLoadingOverlay() {
        rootPane.getChildren().removeIf(node -> "loader".equals(node.getId()));
    }
}
