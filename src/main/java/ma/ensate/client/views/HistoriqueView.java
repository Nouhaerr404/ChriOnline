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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.util.List;

public class HistoriqueView {

    private static final String NAVY       = "#0F172A";
    private static final String ACCENT     = "#3B82F6"; 
    private static final String BG_LIGHT   = "#F1F5F9";
    private static final String TEXT_SUB   = "#64748B";

    private final Stage stage;
    private final ClientTCP clientTCP;
    private final int clientId;
    private final String token;

    public HistoriqueView(Stage stage, ClientTCP clientTCP, int clientId, String token) {
        this.stage = stage;
        this.clientTCP = clientTCP;
        this.clientId = clientId;
        this.token = token;
    }

    private StackPane rootPane;

    public void afficher() {
        rootPane = new StackPane();
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        // Header Dashboard
        VBox header = new VBox(10);
        header.setPadding(new Insets(30, 50, 30, 50));
        header.setStyle("-fx-background-color: " + NAVY + ";");
        
        Button btnBack = new Button("← BOUTIQUE");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT + "; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBack.setOnAction(e -> retourBoutique());

        Label title = new Label("Tableau de bord des commandes");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        header.getChildren().addAll(btnBack, title);
        layout.setTop(header);

        // Content
        VBox container = new VBox(20);
        container.setPadding(new Insets(40, 50, 40, 50));
        container.setAlignment(Pos.TOP_CENTER);
        
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        
        layout.setCenter(scroll);
        rootPane.getChildren().add(layout);

        Scene scene = new Scene(rootPane, 1000, 750);
        stage.setScene(scene);
        stage.show();

        chargerHistorique(container);
    }

    private void chargerHistorique(VBox container) {
        if ("mock-token".equals(token)) {
            Commande c1 = new Commande(); c1.setId("ORD-9921-X"); c1.setPrixAPayer(12000.0); c1.setStatut(ma.ensate.models.StatutCommande.LIVRE);
            Commande c2 = new Commande(); c2.setId("ORD-8832-P"); c2.setPrixAPayer(2500.0); c2.setStatut(ma.ensate.models.StatutCommande.VALIDE);
            container.getChildren().addAll(creerOrderCard(c1), creerOrderCard(c2));
            return;
        }

        new Thread(() -> {
            try {
                Response resp = clientTCP.envoyerRequete(new Request("GET_HISTORIQUE", clientId, token));
                Platform.runLater(() -> {
                    container.getChildren().clear();
                    if (resp.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Commande> orders = (List<Commande>) resp.getData();
                        if (orders == null || orders.isEmpty()) {
                            Label empty = new Label("Aucune commande dans votre compte.");
                            empty.setStyle("-fx-text-fill: " + TEXT_SUB + ";");
                            container.getChildren().add(empty);
                        } else {
                            for (Commande c : orders) {
                                container.getChildren().add(creerOrderCard(c));
                            }
                        }
                    } else {
                        container.getChildren().add(new Label("Erreur : " + resp.getMessage()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> container.getChildren().add(new Label("Erreur de connexion serveur.")));
            }
        }).start();
    }

    private VBox creerOrderCard(Commande c) {
        VBox card = new VBox(15);
        card.setMaxWidth(800);
        card.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 15; -fx-cursor: hand;");
        card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05)));
        
        card.setOnMouseEntered(e -> card.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.1))));
        card.setOnMouseExited(e -> card.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.05))));
        card.setOnMouseClicked(e -> showOrderDetailsPopup(c));
        
        HBox top = new HBox(15);
        top.setAlignment(Pos.CENTER_LEFT);
        
        VBox mid = new VBox(5);
        Label id = new Label("Commande #" + c.getId().substring(0, Math.min(c.getId().length(), 10)).toUpperCase());
        id.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: " + NAVY + ";");
        Label date = new Label("Enregistrée le : " + c.getCommandeDate());
        date.setStyle("-fx-text-fill: " + TEXT_SUB + "; -fx-font-size: 13px;");
        mid.getChildren().addAll(id, date);

        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);

        VBox right = new VBox(5);
        right.setAlignment(Pos.CENTER_RIGHT);
        Label status = new Label(c.getStatut() != null ? c.getStatut().toString() : "INCONNUE");
        String statusColor = "#E2E8F0";
        String textColor = "#475569";
        if (c.getStatut() != null) {
            if (c.getStatut().name().equals("LIVRE")) { statusColor = "#DCFCE7"; textColor = "#166534"; }
            if (c.getStatut().name().equals("VALIDE")) { statusColor = "#DBEAFE"; textColor = "#1E40AF"; }
        }
        
        status.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " + textColor + "; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;");
        
        Label price = new Label(String.format("%.2f MAD", c.getPrixAPayer()));
        price.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + NAVY + ";");
        right.getChildren().addAll(status, price);

        top.getChildren().addAll(mid, s, right);
        card.getChildren().add(top);
        
        return card;
    }

    private void showOrderDetailsPopup(Commande c) {
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6);");
        overlay.setAlignment(Pos.CENTER);

        VBox popup = new VBox(20);
        popup.setMaxWidth(600);
        popup.setMaxHeight(500);
        popup.setPadding(new Insets(30));
        popup.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        popup.setEffect(new DropShadow(20, Color.BLACK));

        Label t = new Label("Détails de la Commande #" + c.getId().substring(0, Math.min(c.getId().length(), 10)).toUpperCase());
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");
        
        Label d = new Label("Date : " + c.getCommandeDate());
        d.setStyle("-fx-text-fill: " + TEXT_SUB + ";");
        
        VBox linesBox = new VBox(10);
        ScrollPane scrollLines = new ScrollPane(linesBox);
        scrollLines.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollLines.setFitToWidth(true);
        scrollLines.setPrefHeight(200);

        if (c.getLignes() != null && !c.getLignes().isEmpty()) {
            for (ma.ensate.models.LigneCommande lc : c.getLignes()) {
                HBox row = new HBox(10);
                Label prod = new Label(lc.getProduit().getNom());
                prod.setStyle("-fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");
                Label qty = new Label("x" + lc.getQuantite());
                qty.setStyle("-fx-text-fill: " + TEXT_SUB + ";");
                Region space = new Region(); HBox.setHgrow(space, Priority.ALWAYS);
                Label sub = new Label(String.format("%.2f MAD", lc.getProduit().getPrix() * lc.getQuantite()));
                sub.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");
                row.getChildren().addAll(prod, qty, space, sub);
                linesBox.getChildren().add(row);
            }
        } else {
            linesBox.getChildren().add(new Label("Détails des articles indisponibles."));
        }

        Separator sep = new Separator();

        HBox totalRow = new HBox();
        Label totL = new Label("Total payé");
        totL.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + NAVY + ";");
        Region space2 = new Region(); HBox.setHgrow(space2, Priority.ALWAYS);
        Label totV = new Label(String.format("%.2f MAD", c.getPrixAPayer()));
        totV.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + ACCENT + ";");
        totalRow.getChildren().addAll(totL, space2, totV);

        Button btnFermer = new Button("FERMER");
        btnFermer.setStyle("-fx-background-color: " + NAVY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 10 30;");
        btnFermer.setOnAction(e -> rootPane.getChildren().remove(overlay));

        HBox bottom = new HBox(btnFermer);
        bottom.setAlignment(Pos.CENTER);

        popup.getChildren().addAll(t, d, new Separator(), scrollLines, sep, totalRow, bottom);
        overlay.getChildren().add(popup);
        rootPane.getChildren().add(overlay);
    }

    private void retourBoutique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Boutique Hub");
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
