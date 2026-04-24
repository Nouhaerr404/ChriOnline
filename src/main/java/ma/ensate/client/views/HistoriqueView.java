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

import java.util.List;

public class HistoriqueView {

    private static final String NAVY       = "#1E293B";
    private static final String ACCENT     = "#F59E0B"; 
    private static final String TEXT_SUB   = "#64748B";

    private final Stage stage;
    private final ClientTCP clientTCP;
    private final int clientId;
    private final String token;

    @FXML private StackPane rootPane;
    @FXML private VBox container;
    @FXML private Label profileInitialLabel;

    public HistoriqueView(Stage stage, ClientTCP clientTCP, int clientId, String token) {
        this.stage = stage;
        this.clientTCP = clientTCP;
        this.clientId = clientId;
        this.token = token;
    }

    public void afficher() {
        try {
            //Crée un objet FXMLLoader qui va charger un fichier FXML (la description visuelle de l'interface)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/historique.fxml"));
            //Définit le contrôleur du FXML comme étant l'objet courant
            loader.setController(this);
            Parent root = loader.load();

            if (stage.getScene() == null) {
                stage.setScene(new Scene(root, 1280, 800));
            } else {
                stage.getScene().setRoot(root);
            }
            stage.setMaximized(true);
            stage.setTitle("ChriOnline — Historique");
            stage.show();

            if (profileInitialLabel != null) {
                profileInitialLabel.setText(buildUserInitial());
            }

            chargerHistorique(container);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        Label err = new Label("INFOS COMPTE : " + resp.getMessage());
                        err.getStyleClass().add("catalog-sidebar-accent");
                        err.setStyle("-fx-text-fill: #ff7ea8; -fx-font-weight: bold;");
                        container.getChildren().add(err);
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
        popup.setMaxHeight(580);
        popup.setPadding(new Insets(35));
        popup.getStyleClass().add("summary-card"); // Utilise le style Neon Noir
        popup.setStyle("-fx-background-color: #0d1b31; -fx-border-color: #1e293b; -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        popup.setEffect(new DropShadow(25, Color.rgb(0,0,0,0.4)));

        Label t = new Label("DÉTAILS COMMANDE");
        t.getStyleClass().add("section-title-accent");
        t.setStyle("-fx-font-size: 18px;");
        
        Label idLab = new Label("#" + c.getId().substring(0, Math.min(c.getId().length(), 12)).toUpperCase());
        idLab.getStyleClass().add("catalog-sidebar-label");

        VBox titleBox = new VBox(5, t, idLab);
        
        Label d = new Label("Enregistrée le : " + c.getCommandeDate());
        d.getStyleClass().add("hero-subtitle");
        d.setStyle("-fx-font-size: 13px;");
        
        VBox linesBox = new VBox(15);
        ScrollPane scrollLines = new ScrollPane(linesBox);
        scrollLines.getStyleClass().add("content-scroll");
        scrollLines.setFitToWidth(true);
        scrollLines.setPrefHeight(250);
        scrollLines.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        if (c.getLignes() != null && !c.getLignes().isEmpty()) {
            for (ma.ensate.models.LigneCommande lc : c.getLignes()) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 10; -fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8;");
                
                Label prod = new Label(lc.getProduit() != null ? lc.getProduit().getNom() : "Produit");
                prod.getStyleClass().add("summary-label");
                prod.setStyle("-fx-font-size: 14px;");
                
                Label qty = new Label("x" + lc.getQuantite());
                qty.getStyleClass().add("catalog-sidebar-label");
                
                Region space = new Region(); HBox.setHgrow(space, Priority.ALWAYS);
                
                double price = lc.getProduit() != null ? lc.getProduit().getPrix() : 0.0;
                Label sub = new Label(String.format("%.2f MAD", price * lc.getQuantite()));
                sub.getStyleClass().add("catalog-sidebar-accent");
                
                row.getChildren().addAll(prod, qty, space, sub);
                linesBox.getChildren().add(row);
            }
        } else {
            linesBox.getChildren().add(new Label("Détails des articles indisponibles."));
        }

        HBox totalRow = new HBox();
        Label totL = new Label("MONTANT TOTAL");
        totL.getStyleClass().add("summary-label");
        Region space2 = new Region(); HBox.setHgrow(space2, Priority.ALWAYS);
        Label totV = new Label(String.format("%.2f MAD", c.getPrixAPayer()));
        totV.getStyleClass().add("summary-total-accent");
        totalRow.getChildren().addAll(totL, space2, totV);

        Button btnFermer = new Button("FERMER LES DÉTAILS");
        btnFermer.getStyleClass().add("secondary-btn");
        btnFermer.setMaxWidth(Double.MAX_VALUE);
        btnFermer.setPrefHeight(50);
        btnFermer.setOnAction(e -> rootPane.getChildren().remove(overlay));

        popup.getChildren().addAll(titleBox, d, new Separator(), scrollLines, new Separator(), totalRow, btnFermer);
        overlay.getChildren().add(popup);
        rootPane.getChildren().add(overlay);
    }

    @FXML
    private void retourBoutique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Boutique Hub");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML
    private void goToPanier() {
        new PanierView(stage, clientTCP, clientId, token).afficher();
    }

    @FXML
    private void goToProfil() {
        System.out.println("[Navigation] Historique -> Profil");
        navigateToProfil();
    }

    @FXML
    private void goToProfilMouse(javafx.scene.input.MouseEvent event) { navigateToProfil(); }

    @FXML
    private void modifierProfil() {
        navigateToProfil();
    }

    private void navigateToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/profil.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Mon Profil");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private String buildUserInitial() {
        String nom = ma.ensate.client.network.SessionManager.getInstance().getNomUtilisateur();
        return (nom != null && !nom.isBlank()) ? nom.trim().substring(0, 1).toUpperCase() : "U";
    }

    @FXML
    private void handleDeconnexion() {
        ma.ensate.client.network.SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Connexion");
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
