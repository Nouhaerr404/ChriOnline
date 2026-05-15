package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.client.security.AdminAuthClient;
import ma.ensate.models.Administrateur;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.security.KeySerializer;
import ma.ensate.security.RSASigner;
import ma.ensate.security.RSAKeyPairGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;

/**
 * View pour l'authentification admin par challenge-response RSA
 */
public class AdminLoginView {

    private static final Logger logger = LogManager.getLogger(AdminLoginView.class);

    @FXML private TextField emailField;
    @FXML private TextField privateKeyPathField;
    @FXML private Button browseButton;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button backToLoginButton;

    private PrivateKey privateKey;

    @FXML
    public void initialize() {
        progressBar.setVisible(false);
    }

    @FXML
    private void handleBrowsePrivateKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner la clé privée RSA");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers clé (*.pem, *.key, *)", "*.*")
        );

        Stage stage = (Stage) emailField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            privateKeyPathField.setText(selectedFile.getAbsolutePath());
            try {
                privateKey = loadPrivateKey(selectedFile);
                messageLabel.setText("Clé privée chargée avec succès");
                messageLabel.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                messageLabel.setText("Erreur chargement clé privée : " + e.getMessage());
                messageLabel.setStyle("-fx-text-fill: red;");
                privateKey = null;
            }
        }
    }

    @FXML
    private void handleAdminLogin() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            afficherErreur("Veuillez entrer votre email admin");
            return;
        }

        if (privateKey == null) {
            afficherErreur("Veuillez sélectionner votre clé privée");
            return;
        }

        loginButton.setDisable(true);
        progressBar.setVisible(true);
        messageLabel.setText("Authentification en cours...");
        messageLabel.setStyle("-fx-text-fill: #1a73e8;");

        new Thread(() -> {
            try {
                // Request challenge
                String challenge = AdminAuthClient.requestChallenge(email);
                
                if (challenge == null) {
                    Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        progressBar.setVisible(false);
                        afficherErreur("Échec de la génération du challenge");
                    });
                    return;
                }

                // Sign the challenge
                String signatureBase64 = RSASigner.signToBase64(challenge, privateKey);
                logger.info("Challenge signé pour admin : " + email);

                // Send to server for verification and get admin with token
                Object[] payload = {email, challenge, signatureBase64};
                Request request = new Request("VERIFY_SIGNATURE_ADMIN", payload);
                Response response = ClientTCP.getInstance().envoyerRequete(request);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    progressBar.setVisible(false);

                    if (response.isSuccess() && response.getData() instanceof Administrateur) {
                        Administrateur admin = (Administrateur) response.getData();
                        String token = admin.getSessionToken();
                        
                        if (token != null) {
                            admin.setSessionToken(token);
                            SessionManager.getInstance().setUtilisateur(admin);
                            logger.info("Token de session admin stocké : " + token);
                            afficherSuccès("Authentification réussie !");
                            ouvrirDashboardAdmin();
                        } else {
                            logger.error("Token null dans la réponse admin");
                            afficherErreur("Erreur: token de session non reçu");
                        }
                    } else {
                        afficherErreur("Échec de l'authentification : " + response.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    progressBar.setVisible(false);
                    afficherErreur("Erreur lors de l'authentification : " + e.getMessage());
                    logger.error("Erreur authentification admin : " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        // Dans l'exécutable Admin, fermer l'application au lieu de retourner vers le login client
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    private PrivateKey loadPrivateKey(File file) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        // Remove PEM headers/footers if present
        content = content.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                        .replace("-----END RSA PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
        return KeySerializer.deserializePrivateKey(content);
    }

    private void ouvrirDashboardAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ma/ensate/fxml/admin_produits.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Dashboard Admin");
        } catch (Exception e) {
            logger.error("Erreur ouverture dashboard admin : " + e.getMessage());
        }
    }

    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    private void afficherSuccès(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: green;");
    }
}
