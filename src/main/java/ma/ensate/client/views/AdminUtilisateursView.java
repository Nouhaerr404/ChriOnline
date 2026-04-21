package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AdminUtilisateursView {

    // ── Colonnes du tableau ───────────────────────────────────────────────────
    @FXML private TableView<Utilisateur>           usersTable;
    @FXML private TableColumn<Utilisateur, Integer> idColumn;
    @FXML private TableColumn<Utilisateur, String>  nomColumn;
    @FXML private TableColumn<Utilisateur, String>  emailColumn;
    @FXML private TableColumn<Utilisateur, String>  typeColumn;
    @FXML private TableColumn<Utilisateur, String>  statutColumn;
    @FXML private TableColumn<Utilisateur, Void>    actionsColumn;

    // ── Labels & status ───────────────────────────────────────────────────────
    @FXML private Label statusLabel;
    @FXML private Label detailNom;
    @FXML private Label detailEmail;
    @FXML private Label detailType;
    @FXML private Label detailStatut;
    @FXML private Label detailAdresse;
    @FXML private Label detailTel;

    // ── Boutons du panneau détail ─────────────────────────────────────────────
    @FXML private Button btnSuspendre;
    @FXML private Button btnReactiver;

    // Utilisateur actuellement sélectionné dans le tableau
    private Utilisateur utilisateurSelectionne;

    // =========================================================================
    // INITIALISATION — appelée automatiquement par JavaFX après chargement FXML
    // =========================================================================
    @FXML
    public void initialize() {

        // ── Configuration des colonnes ────────────────────────────────────────
        idColumn.setCellValueFactory(
                data -> new SimpleObjectProperty<>(data.getValue().getId()));

        nomColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getNom()));

        emailColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getEmail()));

        typeColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getTypeCompte()));

        // Colonne statut avec couleur selon la valeur
        statutColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStatut()));

        statutColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(statut);
                // ACTIF en vert, SUSPENDU en rouge — utilise les couleurs du CSS
                if ("SUSPENDU".equals(statut)) {
                    setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
        });

        // Colonne actions — boutons Suspendre/Réactiver inline dans le tableau
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btnS = new Button("Suspendre");
            private final Button btnR = new Button("Réactiver");
            private final HBox   hbox = new HBox(6, btnS, btnR);
            {
                btnS.getStyleClass().add("danger-btn");
                btnR.getStyleClass().add("primary-btn");

                btnS.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    confirmerSuspension(u);
                });
                btnR.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    confirmerReactivation(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                // Affiche uniquement le bouton pertinent selon le statut
                Utilisateur u = getTableView().getItems().get(getIndex());
                btnS.setVisible("ACTIF".equals(u.getStatut()));
                btnR.setVisible("SUSPENDU".equals(u.getStatut()));
                setGraphic(hbox);
            }
        });

        // Clic sur une ligne → affiche les détails dans le panneau de droite
        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, ancien, nouveau) -> {
                    if (nouveau != null) {
                        afficherDetails(nouveau);
                    }
                }
        );

        // Chargement initial des données
        chargerUtilisateurs();
    }

    // =========================================================================
    // CHARGEMENT DE LA LISTE
    // =========================================================================
    private void chargerUtilisateurs() {
        setStatus("Chargement des utilisateurs...");

        new Thread(() -> {
            List<Utilisateur> liste = fetchUtilisateursDepuisServeur();
            Platform.runLater(() -> {
                usersTable.getItems().setAll(liste);
                setStatus(liste.size() + " utilisateur(s) chargé(s)");
            });
        }).start();
    }

    // =========================================================================
    // AFFICHAGE DU PANNEAU DÉTAIL
    // =========================================================================
    private void afficherDetails(Utilisateur u) {
        utilisateurSelectionne = u;

        detailNom.setText(u.getNom());
        detailEmail.setText(u.getEmail());
        detailType.setText(u.getTypeCompte());
        detailStatut.setText(u.getStatut());

        // Statut en couleur dans le panneau détail
        if ("SUSPENDU".equals(u.getStatut())) {
            detailStatut.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
        } else {
            detailStatut.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }

        // Adresse et téléphone uniquement disponibles pour les clients
        if (u instanceof ma.ensate.models.Client client) {
            detailAdresse.setText(
                    client.getAdresse() != null ? client.getAdresse() : "—");
            detailTel.setText(
                    client.getTel() != null ? client.getTel() : "—");
        } else {
            detailAdresse.setText("—");
            detailTel.setText("—");
        }

        // Affiche uniquement le bouton pertinent selon le statut
        btnSuspendre.setVisible("ACTIF".equals(u.getStatut()));
        btnReactiver.setVisible("SUSPENDU".equals(u.getStatut()));
    }

    // =========================================================================
    // SUSPENDRE UN COMPTE
    // =========================================================================
    @FXML
    private void handleSuspendre() {
        if (utilisateurSelectionne == null) {
            setStatus("Sélectionnez un utilisateur.");
            return;
        }
        confirmerSuspension(utilisateurSelectionne);
    }

    private void confirmerSuspension(Utilisateur u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suspension");
        confirm.setHeaderText("Suspendre le compte de : " + u.getNom());
        confirm.setContentText(
                "L'utilisateur " + u.getEmail() + " ne pourra plus se connecter.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        setStatus("Suspension en cours...");
        new Thread(() -> {
            try {
                Response resp = ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("SUSPENDRE_COMPTE",
                                String.valueOf(u.getId()));

                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        setStatus("Compte de " + u.getNom() + " suspendu.");
                        chargerUtilisateurs(); // recharge le tableau
                    } else {
                        setStatus("Échec : " + resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("Erreur réseau : " + e.getMessage()));
            }
        }).start();
    }

    // =========================================================================
    // RÉACTIVER UN COMPTE
    // =========================================================================
    @FXML
    private void handleReactiver() {
        if (utilisateurSelectionne == null) {
            setStatus("Sélectionnez un utilisateur.");
            return;
        }
        confirmerReactivation(utilisateurSelectionne);
    }

    private void confirmerReactivation(Utilisateur u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la réactivation");
        confirm.setHeaderText("Réactiver le compte de : " + u.getNom());
        confirm.setContentText(
                "L'utilisateur " + u.getEmail() + " pourra à nouveau se connecter.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        setStatus("Réactivation en cours...");
        new Thread(() -> {
            try {
                Response resp = ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("REACTIVER_COMPTE",
                                String.valueOf(u.getId()));

                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        setStatus("Compte de " + u.getNom() + " réactivé.");
                        chargerUtilisateurs();
                    } else {
                        setStatus("Échec : " + resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("Erreur réseau : " + e.getMessage()));
            }
        }).start();
    }

    // =========================================================================
    // NAVIGATION — vers la vue produits admin
    // =========================================================================
    @FXML
    private void handleDashboardPlaceholder() {
        setStatus("Dashboard en développement.");
    }

    @FXML
    private void handleProduits() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/admin_produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Admin Produits");
        } catch (Exception e) {
            setStatus("Erreur navigation : " + e.getMessage());
        }
    }

    @FXML
    private void handleCommandesPlaceholder() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/admin_commandes.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Admin Commandes");
        } catch (Exception e) {
            setStatus("Erreur navigation : " + e.getMessage());
        }
    }

    // =========================================================================
    // DÉCONNEXION — même logique que AdminProduitsView
    // =========================================================================
    @FXML
    private void handleLogout() {
        try {
            Utilisateur current = SessionManager.getInstance().getUtilisateur();
            if (current != null) {
                ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("LOGOUT", current.getId());
            }
            SessionManager.getInstance().clear();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 600));
            stage.setTitle("ChriOnline - Connexion");
        } catch (Exception e) {
            setStatus("Erreur logout : " + e.getMessage());
        }
    }

    // =========================================================================
    // APPEL RÉSEAU — récupère la liste depuis le serveur
    // =========================================================================
    @SuppressWarnings("unchecked")
    private List<Utilisateur> fetchUtilisateursDepuisServeur() {
        try {
            Response resp = ClientTCP.getInstance()
                    .envoyerRequeteSecurisee("LISTER_UTILISATEURS", null);
            if (resp.isSuccess()) {
                return (List<Utilisateur>) resp.getData();
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    // =========================================================================
    // UTILITAIRE
    // =========================================================================
    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}