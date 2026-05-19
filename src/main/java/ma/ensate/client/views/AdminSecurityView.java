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
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.BlockedIP;
import ma.ensate.models.SecurityAlert;
import ma.ensate.models.SecurityLog;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.List;

public class AdminSecurityView {
    private static final Logger logger = LogManager.getLogger(AdminSecurityView.class);

    // Logs
    @FXML private TableView<SecurityLog> logsTable;
    @FXML private TableColumn<SecurityLog, Integer> logIdCol;
    @FXML private TableColumn<SecurityLog, Timestamp> logTimeCol;
    @FXML private TableColumn<SecurityLog, String> logIpCol;
    @FXML private TableColumn<SecurityLog, String> logUserCol;
    @FXML private TableColumn<SecurityLog, String> logActionCol;
    @FXML private TableColumn<SecurityLog, String> logStatusCol;
    @FXML private TableColumn<SecurityLog, String> logDetailsCol;

    // Alerts
    @FXML private TableView<SecurityAlert> alertsTable;
    @FXML private TableColumn<SecurityAlert, Integer> alertIdCol;
    @FXML private TableColumn<SecurityAlert, Timestamp> alertTimeCol;
    @FXML private TableColumn<SecurityAlert, String> alertTypeCol;
    @FXML private TableColumn<SecurityAlert, String> alertSeverityCol;
    @FXML private TableColumn<SecurityAlert, String> alertIpCol;
    @FXML private TableColumn<SecurityAlert, String> alertDescCol;

    // Blocked IPs
    @FXML private TableView<BlockedIP> blockedIpsTable;
    @FXML private TableColumn<BlockedIP, String> blockedIpCol;
    @FXML private TableColumn<BlockedIP, Timestamp> blockedUntilCol;
    @FXML private TableColumn<BlockedIP, String> blockedReasonCol;
    @FXML private TableColumn<BlockedIP, Void> blockedActionsCol;

    @FXML
    public void initialize() {
        setupLogsTable();
        setupAlertsTable();
        setupBlockedIpsTable();

        refreshAll();
    }

    private void setupLogsTable() {
        logIdCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        logTimeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getTimestamp()));
        logIpCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIpAddress()));
        logUserCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserIdentifier()));
        logActionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActionType()));
        logStatusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        logDetailsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDetails()));
        
        logStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("FAILURE".equals(status)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if ("WARNING".equals(status)) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else if ("SUCCESS".equals(status)) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupAlertsTable() {
        alertIdCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        alertTimeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getTimestamp()));
        alertTypeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlertType()));
        alertSeverityCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSeverity()));
        alertIpCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTargetIp()));
        alertDescCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        alertSeverityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty || severity == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(severity);
                    if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if ("MEDIUM".equals(severity)) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3498db;");
                    }
                }
            }
        });
    }

    private void setupBlockedIpsTable() {
        blockedIpCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIpAddress()));
        blockedUntilCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBlockedUntil()));
        blockedReasonCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));

        blockedActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button btnUnblock = new Button("Débloquer");
            {
                btnUnblock.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                btnUnblock.setOnAction(e -> {
                    BlockedIP blocked = getTableView().getItems().get(getIndex());
                    unblockIP(blocked.getIpAddress());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnUnblock);
                }
            }
        });
    }

    @FXML
    public void refreshAll() {
        new Thread(() -> {
            try {
                // Fetch Logs
                Response resLogs = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_SECURITY_LOGS", null);
                if (resLogs.isSuccess()) {
                    List<SecurityLog> logs = (List<SecurityLog>) resLogs.getData();
                    Platform.runLater(() -> logsTable.setItems(FXCollections.observableArrayList(logs)));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Logs", resLogs.getMessage()));
                }

                // Fetch Alerts
                Response resAlerts = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_IDS_ALERTS", null);
                if (resAlerts.isSuccess()) {
                    List<SecurityAlert> alerts = (List<SecurityAlert>) resAlerts.getData();
                    Platform.runLater(() -> alertsTable.setItems(FXCollections.observableArrayList(alerts)));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Alerts", resAlerts.getMessage()));
                }

                // Fetch Blocked IPs
                Response resIps = ClientTCP.getInstance().envoyerRequeteSecurisee("GET_BLOCKED_IPS", null);
                if (resIps.isSuccess()) {
                    List<BlockedIP> ips = (List<BlockedIP>) resIps.getData();
                    Platform.runLater(() -> blockedIpsTable.setItems(FXCollections.observableArrayList(ips)));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur IPs", resIps.getMessage()));
                }
            } catch (Exception e) {
                logger.error("Erreur refresh : " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Exception Fatale Refresh", e.toString() + " : " + e.getMessage()));
            }
        }).start();
    }

    private void unblockIP(String ip) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Êtes-vous sûr de vouloir débloquer l'IP " + ip + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    Response res = ClientTCP.getInstance().envoyerRequeteSecurisee("UNBLOCK_IP", ip);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            refreshAll();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur", res.getMessage());
                        }
                    });
                } catch (Exception e) {
                    logger.error("Erreur unblock : " + e.getMessage());
                }
            }).start();
        }
    }

    @FXML
    public void handleProduits() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Admin Produits");
        } catch (Exception e) {
            logger.error("Erreur navigation: " + e.getMessage());
        }
    }

    @FXML
    public void handleUtilisateurs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_utilisateurs.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Gestion des Utilisateurs");
        } catch (Exception e) {
            logger.error("Erreur navigation: " + e.getMessage());
        }
    }

    @FXML
    public void handleCommandesPlaceholder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_commandes.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline - Gestion des Commandes");
        } catch (Exception e) {
            logger.error("Erreur navigation commandes: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Request req = new Request("LOGOUT", null);
            ClientTCP.getInstance().envoyerRequete(req);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ma/ensate/fxml/admin_login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logsTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
        } catch (Exception e) {
            logger.error("Erreur deconnexion : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
