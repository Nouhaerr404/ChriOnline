package ma.ensate.client.views;

import ma.ensate.client.network.ClientTCP;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PanierView {

    // ── Palette Apple Store ───────────────────────────────────────────────────
    private static final String BG_PAGE    = "#F5F5F7";
    private static final String BG_WHITE   = "#FFFFFF";
    private static final String BG_NAV     = "#FFFFFF";
    private static final String BLUE       = "#0071E3";
    private static final String BLUE_DARK  = "#0051A2";
    private static final String TEXT_DARK  = "#1D1D1F";
    private static final String TEXT_MID   = "#6E6E73";
    private static final String TEXT_LIGHT = "#AEAEB2";
    private static final String RED        = "#FF3B30";
    private static final String GREEN      = "#34C759";
    private static final String BORDER     = "#D2D2D7";
    private static final String BORDER_LIGHT = "#F0F0F5";

    // ── DTO ───────────────────────────────────────────────────────────────────
    public static class LigneTableau {
        private int produitId; private String nomProduit;
        private double prixUnitaire; private int quantite; private double subtotal;

        public LigneTableau(int produitId, String nomProduit, double prixUnitaire, int quantite, double subtotal) {
            this.produitId=produitId; this.nomProduit=nomProduit;
            this.prixUnitaire=prixUnitaire; this.quantite=quantite; this.subtotal=subtotal;
        }
        public int    getProduitId()      { return produitId; }
        public String getNomProduit()     { return nomProduit; }
        public double getPrixUnitaire()   { return prixUnitaire; }
        public int    getQuantite()       { return quantite; }
        public double getSubtotal()       { return subtotal; }
        public void   setQuantite(int q)  { this.quantite = q; }
        public void   setSubtotal(double s){ this.subtotal = s; }
    }

    // ── Attributs ─────────────────────────────────────────────────────────────
    private final Stage     stage;
    private final ClientTCP clientTCP;
    private final int       clientId;
    private final String    token;

    private TableView<LigneTableau>      tableView;
    private ObservableList<LigneTableau> lignes;
    private Label labelTotal, labelMessage, labelCount, labelSubtitleCount;

    public PanierView(Stage stage, ClientTCP clientTCP, int clientId, String token) {
        this.stage     = stage;
        this.clientTCP = clientTCP;
        this.clientId  = clientId;
        this.token     = token;
        this.lignes    = FXCollections.observableArrayList();
    }

    // ── Affichage ─────────────────────────────────────────────────────────────
    public void afficher() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG_PAGE + ";");
        root.setTop(construireNavBar());
        root.setCenter(construireCentre());
        root.setBottom(construireFooter());

        Scene scene = new Scene(root, 980, 640);
        scene.setFill(Color.web(BG_PAGE));
        stage.setTitle("ChriOnline — Mon Panier");
        stage.setScene(scene);
        stage.show();
        chargerPanier();
    }

    // ── NavBar ────────────────────────────────────────────────────────────────
    private HBox construireNavBar() {
        HBox nav = new HBox();
        nav.setStyle(
                "-fx-background-color:" + BG_NAV + ";" +
                        "-fx-border-color:" + BORDER_LIGHT + ";" +
                        "-fx-border-width:0 0 1 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);"
        );
        nav.setPadding(new Insets(16, 32, 16, 32));
        nav.setAlignment(Pos.CENTER_LEFT);

        // Brand
        Label brand = new Label("ChriOnline");
        brand.setStyle(
                "-fx-font-family:'SF Pro Display', 'Helvetica Neue', Arial;" +
                        "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";"
        );

        // Diviseur
        Region div = new Region();
        div.setStyle("-fx-background-color:" + BORDER + ";");
        div.setPrefSize(1, 20);
        HBox.setMargin(div, new Insets(0, 20, 0, 20));

        // Titre page
        Label titrePage = new Label("Panier");
        titrePage.setStyle(
                "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                        "-fx-font-size:16px;-fx-text-fill:" + TEXT_MID + ";"
        );

        // Badge count
        labelCount = new Label("0");
        labelCount.setStyle(
                "-fx-background-color:" + BLUE + ";-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-padding:2 7 2 7;-fx-background-radius:20;"
        );
        HBox.setMargin(labelCount, new Insets(0, 0, 0, 8));

        // Message statut
        labelMessage = new Label("");
        labelMessage.setStyle(
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-font-size:12px;-fx-text-fill:" + TEXT_MID + ";"
        );
        HBox.setMargin(labelMessage, new Insets(0, 0, 0, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton Historique
        Button btnHistorique = new Button("Mes Commandes");
        btnHistorique.setStyle(btnGhost());
        btnHistorique.setOnMouseEntered(e -> btnHistorique.setStyle(btnGhostHover()));
        btnHistorique.setOnMouseExited(e -> btnHistorique.setStyle(btnGhost()));
        btnHistorique.setOnAction(e -> {
            new HistoriqueView(stage, clientTCP, clientId, token).afficher();
        });
        HBox.setMargin(btnHistorique, new Insets(0, 10, 0, 0));

        // Bouton retour
        Button btnRetour = new Button("← Continuer mes achats");
        btnRetour.setStyle(btnGhost());
        btnRetour.setOnMouseEntered(e -> btnRetour.setStyle(btnGhostHover()));
        btnRetour.setOnMouseExited(e -> btnRetour.setStyle(btnGhost()));
        btnRetour.setOnAction(e -> retourProduits());

        nav.getChildren().addAll(brand, div, titrePage, labelCount, labelMessage, spacer, btnHistorique, btnRetour);
        return nav;
    }

    // ── Centre ────────────────────────────────────────────────────────────────
    private VBox construireCentre() {
        // En-tête section
        HBox secH = new HBox(10);
        secH.setAlignment(Pos.CENTER_LEFT);
        secH.setPadding(new Insets(28, 32, 16, 32));

        Label titreSection = new Label("Articles sélectionnés");
        titreSection.setStyle(
                "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                        "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";"
        );

        labelSubtitleCount = new Label("0 article");
        labelSubtitleCount.setStyle(
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-font-size:14px;-fx-text-fill:" + TEXT_MID + ";"
        );

        secH.getChildren().addAll(titreSection, labelSubtitleCount);

        // Tableau
        tableView = new TableView<>(lignes);
        tableView.setPlaceholder(placeholderVide());
        tableView.setStyle(
                "-fx-background-color:" + BG_WHITE + ";" +
                        "-fx-border-color:" + BORDER + ";-fx-border-radius:12;" +
                        "-fx-background-radius:12;-fx-border-width:1;" +
                        "-fx-table-cell-border-color:" + BORDER_LIGHT + ";" +
                        "-fx-selection-bar:#E8F0FE;-fx-selection-bar-non-focused:#F0F4FF;"
        );
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getColumns().addAll(colNom(), colPrix(), colQte(), colSub(), colDel());

        VBox wrapper = new VBox(tableView);
        wrapper.setPadding(new Insets(0, 32, 0, 32));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        VBox centre = new VBox(secH, wrapper);
        centre.setStyle("-fx-background-color:" + BG_PAGE + ";");
        centre.setPadding(new Insets(0, 0, 20, 0));
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        VBox.setVgrow(centre, Priority.ALWAYS);
        return centre;
    }

    // ── Colonnes ──────────────────────────────────────────────────────────────
    private TableColumn<LigneTableau, String> colNom() {
        TableColumn<LigneTableau, String> col = new TableColumn<>("Produit");
        col.setCellValueFactory(new PropertyValueFactory<>("nomProduit"));
        col.setPrefWidth(300);
        col.setStyle("-fx-alignment:CENTER-LEFT;");
        col.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                VBox box = new VBox(2);
                Label nom = new Label(item);
                nom.setStyle(
                        "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";"
                );
                Label cat = new Label("Électronique");
                cat.setStyle(
                        "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                                "-fx-font-size:11px;-fx-text-fill:" + TEXT_LIGHT + ";"
                );
                box.getChildren().addAll(nom, cat);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        return col;
    }

    private TableColumn<LigneTableau, Double> colPrix() {
        TableColumn<LigneTableau, Double> col = new TableColumn<>("Prix unit.");
        col.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        col.setPrefWidth(130);
        col.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(String.format("%.2f MAD", item));
                lbl.setStyle(
                        "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                                "-fx-font-size:13px;-fx-text-fill:" + TEXT_MID + ";"
                );
                setGraphic(lbl);
            }
        });
        return col;
    }

    private TableColumn<LigneTableau, Integer> colQte() {
        TableColumn<LigneTableau, Integer> col = new TableColumn<>("Quantité");
        col.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        col.setPrefWidth(150);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button btnM = mkBtnQte("−", RED);
            private final Label  lblQ = new Label();
            private final Button btnP = mkBtnQte("+", GREEN);
            private final HBox   hbox = new HBox(10, btnM, lblQ, btnP);
            {
                hbox.setAlignment(Pos.CENTER);
                lblQ.setStyle(
                        "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                                "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";" +
                                "-fx-min-width:30px;-fx-alignment:CENTER;"
                );
                btnM.setOnAction(e -> {
                    LigneTableau l = getTableView().getItems().get(getIndex());
                    envoyerModificationQuantite(l.getProduitId(), l.getQuantite() - 1);
                });
                btnP.setOnAction(e -> {
                    LigneTableau l = getTableView().getItems().get(getIndex());
                    envoyerModificationQuantite(l.getProduitId(), l.getQuantite() + 1);
                });
            }
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                lblQ.setText(String.valueOf(item));
                setGraphic(hbox);
            }
        });
        return col;
    }

    private TableColumn<LigneTableau, Double> colSub() {
        TableColumn<LigneTableau, Double> col = new TableColumn<>("Sous-total");
        col.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        col.setPrefWidth(150);
        col.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(String.format("%.2f MAD", item));
                lbl.setStyle(
                        "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + BLUE + ";"
                );
                setGraphic(lbl);
            }
        });
        return col;
    }

    private TableColumn<LigneTableau, Void> colDel() {
        TableColumn<LigneTableau, Void> col = new TableColumn<>("");
        col.setPrefWidth(80);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button("Retirer");
            {
                btn.setStyle(btnRemove());
                btn.setOnMouseEntered(e -> btn.setStyle(btnRemoveHover()));
                btn.setOnMouseExited(e -> btn.setStyle(btnRemove()));
                btn.setOnAction(e -> {
                    LigneTableau l = getTableView().getItems().get(getIndex());
                    confirmerSuppression(l);
                });
            }
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        return col;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private HBox construireFooter() {
        HBox footer = new HBox();
        footer.setStyle(
                "-fx-background-color:" + BG_WHITE + ";" +
                        "-fx-border-color:" + BORDER_LIGHT + ";-fx-border-width:1 0 0 0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,-2);"
        );
        footer.setPadding(new Insets(20, 32, 20, 32));
        footer.setAlignment(Pos.CENTER_LEFT);

        // Bloc total
        VBox blocTotal = new VBox(3);
        Label totalLabel = new Label("Total de la commande");
        totalLabel.setStyle(
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-font-size:12px;-fx-text-fill:" + TEXT_MID + ";"
        );
        labelTotal = new Label("0,00 MAD");
        labelTotal.setStyle(
                "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                        "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";"
        );
        Label taxeNote = new Label("TVA incluse");
        taxeNote.setStyle(
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-font-size:11px;-fx-text-fill:" + TEXT_LIGHT + ";"
        );
        blocTotal.getChildren().addAll(totalLabel, labelTotal, taxeNote);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton vider
        Button btnVider = new Button("Vider le panier");
        btnVider.setStyle(btnGhost());
        btnVider.setOnMouseEntered(e -> btnVider.setStyle(btnGhostDanger()));
        btnVider.setOnMouseExited(e -> btnVider.setStyle(btnGhost()));
        btnVider.setOnAction(e -> viderPanier());

        Region gap = new Region(); gap.setPrefWidth(12);

        // Bouton commander
        Button btnValider = new Button("Commander →");
        btnValider.setStyle(btnPrimary());
        btnValider.setOnMouseEntered(e -> btnValider.setStyle(btnPrimaryHover()));
        btnValider.setOnMouseExited(e -> btnValider.setStyle(btnPrimary()));
        btnValider.setOnAction(e -> validerCommande());

        footer.getChildren().addAll(blocTotal, spacer, btnVider, gap, btnValider);
        return footer;
    }

    // ── Placeholder ───────────────────────────────────────────────────────────
    private VBox placeholderVide() {
        Label ico = new Label("🛒");
        ico.setStyle("-fx-font-size:48px;");
        Label msg = new Label("Votre panier est vide");
        msg.setStyle(
                "-fx-font-family:'SF Pro Display','Helvetica Neue',Arial;" +
                        "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";"
        );
        Label sub = new Label("Ajoutez des produits pour commencer");
        sub.setStyle(
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-font-size:13px;-fx-text-fill:" + TEXT_MID + ";"
        );
        VBox box = new VBox(12, ico, msg, sub);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ── Actions réseau ────────────────────────────────────────────────────────
    private void chargerPanier() {
        afficherMessage("Chargement...", false);
        new Thread(() -> {
            try {
                Response r = clientTCP.envoyerRequete(
                        new Request("AFFICHER_PANIER", String.valueOf(clientId), token));
                Platform.runLater(() -> {
                    if (r != null && r.isSuccess()) {
                        parserEtAfficherPanier((String) r.getData());
                        afficherMessage("", false);
                    } else {
                        afficherMessage("Impossible de charger le panier.", true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> afficherMessage("Erreur réseau : " + e.getMessage(), true));
            }
        }).start();
    }

    private void envoyerModificationQuantite(int produitId, int qte) {
        new Thread(() -> {
            try {
                Response r = clientTCP.envoyerRequete(
                        new Request("MODIFIER_QUANTITE_PANIER", clientId + "," + produitId + "," + qte, token));
                Platform.runLater(() -> {
                    if (r != null && r.isSuccess()) chargerPanier();
                    else afficherMessage(r != null ? r.getMessage() : "Erreur", true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> afficherMessage("Erreur réseau", true));
            }
        }).start();
    }

    private void confirmerSuppression(LigneTableau ligne) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Retirer l'article");
        a.setHeaderText(null);
        a.setContentText("Retirer « " + ligne.getNomProduit() + " » du panier ?");
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) supprimerProduit(ligne.getProduitId());
        });
    }

    private void supprimerProduit(int produitId) {
        new Thread(() -> {
            try {
                Response r = clientTCP.envoyerRequete(
                        new Request("SUPPRIMER_DU_PANIER", clientId + "," + produitId, token));
                Platform.runLater(() -> {
                    if (r != null && r.isSuccess()) chargerPanier();
                    else afficherMessage("Erreur suppression", true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> afficherMessage("Erreur réseau", true));
            }
        }).start();
    }

    private void viderPanier() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Vider le panier");
        a.setHeaderText(null);
        a.setContentText("Supprimer tous les articles du panier ?");
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Response resp = clientTCP.envoyerRequete(
                                new Request("VIDER_PANIER", String.valueOf(clientId), token));
                        Platform.runLater(() -> {
                            if (resp != null && resp.isSuccess()) {
                                lignes.clear();
                                mettreAJourTotal(0.0);
                                labelCount.setText("0");
                                labelSubtitleCount.setText("0 article");
                            } else afficherMessage("Erreur", true);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> afficherMessage("Erreur réseau", true));
                    }
                }).start();
            }
        });
    }

    private void validerCommande() {
        if (lignes.isEmpty()) { afficherMessage("Votre panier est vide.", true); return; }
        afficherMessage("Redirection vers la commande...", false);
        new CommandeView(stage, clientTCP, clientId, token, 
            Double.parseDouble(labelTotal.getText().replace(" MAD", "").replace(",", ".")), 
            new ArrayList<>(lignes)).afficher();
    }

    private void retourProduits() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Boutique");
        } catch (Exception e) {
            e.printStackTrace();
            afficherMessage("Erreur lors du retour : " + e.getMessage(), true);
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────────
    private void parserEtAfficherPanier(String data) {
        lignes.clear();
        if (data == null || data.isBlank()) { mettreAJourTotal(0.0); return; }
        double total = 0.0;
        double calculatedTotal = 0.0;
        for (String row : data.split("\n")) {
            if (row.startsWith("TOTAL:")) {
                try { total = Double.parseDouble(row.substring(6).trim().replace(",", ".")); }
                catch (NumberFormatException ignored) {}
                continue;
            }
            String[] p = row.split("\\|");
            if (p.length < 5) continue;
            try {
                LigneTableau lt = new LigneTableau(
                        Integer.parseInt(p[0].trim()), p[1].trim(),
                        Double.parseDouble(p[2].trim().replace(",", ".")),
                        Integer.parseInt(p[3].trim()),
                        Double.parseDouble(p[4].trim().replace(",", ".")));
                lignes.add(lt);
                calculatedTotal += lt.getSubtotal();
            } catch (NumberFormatException ignored) {}
        }
        
        if (total == 0.0) total = calculatedTotal;
        mettreAJourTotal(total);
        int n = lignes.size();
        labelCount.setText(String.valueOf(n));
        labelSubtitleCount.setText("  —  " + n + (n > 1 ? " articles" : " article"));
    }

    private void mettreAJourTotal(double total) {
        labelTotal.setText(String.format("%.2f MAD", total));
    }

    private void afficherMessage(String msg, boolean erreur) {
        labelMessage.setText(msg);
        labelMessage.setStyle(
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                        "-fx-font-size:12px;-fx-text-fill:" + (erreur ? RED : TEXT_MID) + ";"
        );
    }

    // ── Styles boutons ────────────────────────────────────────────────────────
    private Button mkBtnQte(String label, String color) {
        Button btn = new Button(label);
        String base =
                "-fx-background-color:white;-fx-border-color:" + BORDER + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;" +
                        "-fx-background-radius:8;-fx-text-fill:" + color + ";" +
                        "-fx-font-size:16px;-fx-font-weight:bold;-fx-cursor:hand;" +
                        "-fx-min-width:30px;-fx-min-height:30px;-fx-padding:0;";
        String hover =
                "-fx-background-color:" + BG_PAGE + ";-fx-border-color:" + color + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-text-fill:" + color + ";-fx-font-size:16px;-fx-font-weight:bold;" +
                        "-fx-cursor:hand;-fx-min-width:30px;-fx-min-height:30px;-fx-padding:0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private String btnPrimary() {
        return "-fx-background-color:" + BLUE + ";-fx-text-fill:white;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-border-radius:20;" +
                "-fx-background-radius:20;-fx-cursor:hand;-fx-padding:12 28 12 28;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,113,227,0.35),10,0,0,3);";
    }

    private String btnPrimaryHover() {
        return "-fx-background-color:" + BLUE_DARK + ";-fx-text-fill:white;" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-border-radius:20;" +
                "-fx-background-radius:20;-fx-cursor:hand;-fx-padding:12 28 12 28;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,81,162,0.4),12,0,0,4);";
    }

    private String btnGhost() {
        return "-fx-background-color:transparent;-fx-border-color:" + BORDER + ";" +
                "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;" +
                "-fx-text-fill:" + TEXT_MID + ";" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13px;-fx-cursor:hand;-fx-padding:10 20 10 20;";
    }

    private String btnGhostHover() {
        return "-fx-background-color:" + BG_PAGE + ";-fx-border-color:" + BORDER + ";" +
                "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;" +
                "-fx-text-fill:" + TEXT_DARK + ";" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13px;-fx-cursor:hand;-fx-padding:10 20 10 20;";
    }

    private String btnGhostDanger() {
        return "-fx-background-color:transparent;-fx-border-color:" + RED + ";" +
                "-fx-border-width:1;-fx-border-radius:20;-fx-background-radius:20;" +
                "-fx-text-fill:" + RED + ";" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:13px;-fx-cursor:hand;-fx-padding:10 20 10 20;";
    }

    private String btnRemove() {
        return "-fx-background-color:transparent;-fx-border-color:transparent;" +
                "-fx-text-fill:" + TEXT_LIGHT + ";" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:4 8 4 8;";
    }

    private String btnRemoveHover() {
        return "-fx-background-color:#FFF2F1;-fx-border-color:transparent;" +
                "-fx-text-fill:" + RED + ";" +
                "-fx-font-family:'SF Pro Text','Helvetica Neue',Arial;" +
                "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:4 8 4 8;" +
                "-fx-background-radius:6;";
    }
}