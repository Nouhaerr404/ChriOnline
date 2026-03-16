package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Client;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegisterView {

    private static final Logger logger = LogManager.getLogger(RegisterView.class);

    @FXML private TextField     nomField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     adresseField;
    @FXML private TextField     telField;
    @FXML private Label         messageLabel;
    @FXML private Button        registerButton;

    // =============================================
    // HANDLE REGISTER
    // =============================================
    @FXML
    private void handleRegister() {
        String nom      = nomField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String adresse  = adresseField.getText().trim();
        String tel      = telField.getText().trim();

        // Validation côté client
        if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            afficherErreur("Nom, email et mot de passe sont obligatoires !");
            return;
        }

        if (password.length() < 6) {
            afficherErreur("Le mot de passe doit contenir au moins 6 caracteres !");
            return;
        }

        // Désactiver le bouton
        registerButton.setDisable(true);
        afficherInfo("Inscription en cours...");

        // Envoyer dans un thread séparé
        new Thread(() -> {
            try {
                // Créer l'objet Client
                Client client = new Client(nom, email, password, adresse, tel);

                // Créer la requête
                Request request = new Request("REGISTER", client);

                // Envoyer au serveur
                Response response = ClientTCP.getInstance()
                        .envoyerRequete(request);

                Platform.runLater(() -> {
                    registerButton.setDisable(false);

                    if (response.isSuccess()) {
                        afficherSucces(" Inscription reussie ! Redirection...");
                        logger.info(" Inscription reussie : " + email);

                        // Attendre 1 seconde puis aller au login
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(this::allerLogin);
                            } catch (InterruptedException ignored) {}
                        }).start();

                    } else {
                        afficherErreur(response.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    afficherErreur("Impossible de contacter le serveur.");
                    logger.error("Erreur register : " + e.getMessage());
                });
            }
        }).start();
    }

    // =============================================
    // RETOUR AU LOGIN
    // =============================================
    @FXML
    private void allerLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 500));
            stage.setTitle("ChriOnline — Connexion");
        } catch (Exception e) {
            logger.error("Erreur navigation vers login : " + e.getMessage());
        }
    }

    // =============================================
    // UTILITAIRES
    // =============================================
    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
    }

    private void afficherSucces(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
    }

    private void afficherInfo(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #1a73e8; -fx-font-size: 12px;");
    }
}