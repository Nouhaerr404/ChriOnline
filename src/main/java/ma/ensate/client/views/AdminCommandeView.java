package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.StatutCommande;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.ChangerStatutRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminCommandeView {

    @FXML
    private TableView<Commande> commandesTable;
    @FXML
    private TableColumn<Commande, String> idColumn;
    @FXML
    private TableColumn<Commande, String> clientColumn;
    @FXML
    private TableColumn<Commande, String> dateColumn;
    @FXML
    private TableColumn<Commande, Double> prixColumn;
    @FXML
    private TableColumn<Commande, String> detailsColumn;
    @FXML
    private TableColumn<Commande, StatutCommande> statutColumn;
    @FXML
    private Label statusLabel;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() { // — appelée automatiquement au chargement du fxml

        // --- Configuration des colonnes (Style direct simple) ---
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));

        clientColumn.setCellValueFactory(data -> {
            Utilisateur c = data.getValue().getClient();
            return new SimpleStringProperty(c != null ? c.getNom() : "Client Inconnu");
        });

        dateColumn.setCellValueFactory(data -> {
            if (data.getValue().getCommandeDate() != null) {
                return new SimpleStringProperty(data.getValue().getCommandeDate().format(formatter));
            }
            return new SimpleStringProperty("");
        });

        prixColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPrixAPayer()));

        detailsColumn.setCellValueFactory(data -> {
            List<LigneCommande> lignes = data.getValue().getLignes();
            if (lignes == null || lignes.isEmpty())
                return new SimpleStringProperty("Aucun article");

            StringBuilder sb = new StringBuilder();
            for (LigneCommande l : lignes) {
                sb.append(l.getQuantite()).append("x ").append(l.getProduitNom()).append(", ");
            }
            String res = sb.toString();
            return new SimpleStringProperty(res.substring(0, res.length() - 2));
        });

        // Colonne Statut modifiable
        statutColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatut()));
        statutColumn.setCellFactory(ComboBoxTableCell.forTableColumn(StatutCommande.values()));
        statutColumn.setOnEditCommit(event -> {
            StatutCommande nouveau = event.getNewValue();
            if (nouveau != null)
                mettreAJourStatut(event.getRowValue(), nouveau);
        });

        commandesTable.setEditable(true);
        chargerDonnees();
    }

    private void chargerDonnees() {
        setStatus("Chargement des données en cours...");
        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_ALL_COMMANDES", null);
                if (response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Commande> list = (List<Commande>) response.getData();
                    Platform.runLater(() -> {
                        commandesTable.setItems(FXCollections.observableArrayList(list));
                        setStatus(list.size() + " commandes récupérées.");
                    });
                } else {
                    Platform.runLater(() -> setStatus("Erreur: " + response.getMessage()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Erreur de connexion au serveur."));
            }
        }).start();
    }

    private void mettreAJourStatut(Commande cmd, StatutCommande nouveau) {
        setStatus("Mise à jour du statut...");
        new Thread(() -> {
            try {
                ChangerStatutRequest req = new ChangerStatutRequest(cmd.getId(), nouveau.name());
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("CHANGER_STATUT_COMMANDE", req);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        cmd.setStatut(nouveau);
                        setStatus("Statut modifié avec succès !");
                    } else {
                        setStatus("Échec: " + response.getMessage());
                        commandesTable.refresh();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("Erreur réseau.");
                    commandesTable.refresh();
                });
            }
        }).start();
    }

    // --- Navigation et Header ---

    @FXML
    public void refreshOrders() {
        chargerDonnees();
    }

    @FXML
    public void handleDashboardPlaceholder() {
        setStatus("Dashboard en développement.");
    }

    @FXML
    public void handleProduits() {
        naviguer("/ma/ensate/fxml/admin_produits.fxml", "Admin Produits");
    }

    @FXML
    public void handleUtilisateurs() {
        naviguer("/ma/ensate/fxml/admin_utilisateurs.fxml", "Gestion des Utilisateurs");
    }

    @FXML
    public void handleBack() {
        naviguer("/ma/ensate/fxml/admin_produits.fxml", "Dashboard Admin Produits");
    }

    @FXML
    public void handleHistoriquePlaceholder() {
        setStatus("L'historique sera bientôt disponible.");
    }

    @FXML
    public void handleLogout() {
        try {
            Utilisateur current = SessionManager.getInstance().getUtilisateur();
            if (current != null)
                ClientTCP.getInstance().envoyerRequeteSecurisee("LOGOUT", current.getId());
            SessionManager.getInstance().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) commandesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Connexion Admin");
        } catch (Exception e) {
            setStatus("Erreur déconnexion.");
        }
    }

    private void naviguer(String fxml, String titre) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) commandesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(titre);
        } catch (Exception e) {
            setStatus("Erreur de navigation : " + e.getMessage());
        }
    }

    @FXML
    public void handleSecurity() {
        naviguer("/ma/ensate/fxml/admin_security.fxml", "ChriOnline — Supervision Sécurité (IDS/IPS)");
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}
