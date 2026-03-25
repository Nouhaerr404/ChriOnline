package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.StatutCommande;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.ChangerStatutRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommandeView {

    @FXML private TableView<Commande> commandesTable;
    @FXML private TableColumn<Commande, String> idColumn;
    @FXML private TableColumn<Commande, String> clientColumn;
    @FXML private TableColumn<Commande, String> dateColumn;
    @FXML private TableColumn<Commande, Double> prixColumn;
    @FXML private TableColumn<Commande, String> detailsColumn;
    @FXML private TableColumn<Commande, StatutCommande> statutColumn;
    @FXML private Label statusLabel;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadAllCommandes();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        
        clientColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getClient() != null ? data.getValue().getClient().getNom() : "Inconnu"
        ));
        
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCommandeDate() != null ? data.getValue().getCommandeDate().format(formatter) : ""
        ));
        
        prixColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPrixAPayer()));
        
        detailsColumn.setCellValueFactory(data -> {
            List<LigneCommande> lignes = data.getValue().getLignes();
            if (lignes == null || lignes.isEmpty()) return new SimpleStringProperty("Aucun article");
            String summary = lignes.stream()
                    .map(l -> l.getQuantite() + "x " + (l.getProduitNom() != null ? l.getProduitNom() : "Produit #" + l.getProduit().getId()))
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(summary);
        });

        // Configurer la colonne Statut avec une ComboBox
        statutColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatut()));
        statutColumn.setCellFactory(column -> {
            ComboBoxTableCell<Commande, StatutCommande> cell = new ComboBoxTableCell<>(StatutCommande.values());
            cell.setComboBoxEditable(false);
            
            // Styliser la cellule
            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    switch (newVal) {
                        case EN_ATTENTE: cell.setTextFill(Color.web("#F59E0B")); break;
                        case VALIDE: cell.setTextFill(Color.web("#10B981")); break;
                        case EXPEDIE: cell.setTextFill(Color.web("#3B82F6")); break;
                        case LIVRE: cell.setTextFill(Color.web("#6366F1")); break;
                    }
                    cell.setStyle("-fx-font-weight: bold;");
                }
            });
            
            return cell;
        });

        // Gérer le changement de valeur dans la ComboBox
        statutColumn.setOnEditCommit(event -> {
            Commande cmd = event.getRowValue();
            StatutCommande nouveauStatut = event.getNewValue();
            updateCommandeStatut(cmd, nouveauStatut);
        });

        commandesTable.setEditable(true);
    }

    private void loadAllCommandes() {
        setStatus("Chargement des commandes...");
        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_ALL_COMMANDES", null);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Commande> list = (List<Commande>) response.getData();
                        commandesTable.setItems(FXCollections.observableArrayList(list));
                        setStatus(list.size() + " commandes chargées.");
                    } else {
                        setStatus("Erreur: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Erreur réseau: " + e.getMessage()));
            }
        }).start();
    }

    private void updateCommandeStatut(Commande cmd, StatutCommande nouveauStatut) {
        if (cmd.getStatut() == nouveauStatut) return;
        
        setStatus("Mise à jour du statut...");
        new Thread(() -> {
            try {
                ChangerStatutRequest req = new ChangerStatutRequest(cmd.getId(), nouveauStatut.name());
                Response response = ClientTCP.getInstance().envoyerRequeteSecurisee("CHANGER_STATUT_COMMANDE", req);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        cmd.setStatut(nouveauStatut);
                        commandesTable.refresh();
                        setStatus("Statut mis à jour avec succès.");
                    } else {
                        setStatus("Échec: " + response.getMessage());
                        commandesTable.refresh(); // Remettre l'ancien statut visuellement
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("Erreur réseau: " + e.getMessage());
                    commandesTable.refresh();
                });
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadAllCommandes();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) commandesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            setStatus("Erreur lors du retour: " + e.getMessage());
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}
