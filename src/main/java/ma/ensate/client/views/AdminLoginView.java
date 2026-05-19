package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.client.security.AdminAuthClient;
import ma.ensate.models.Administrateur;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import ma.ensate.security.KeyStoreManager;
import ma.ensate.security.RSASigner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.security.PrivateKey;

/**
 * Vue pour l'authentification admin par challenge-response RSA.
 * Utilise un Keystore PKCS12 (.p12) pour stocker la clé privée de l'admin,
 * au lieu d'un simple fichier PEM en texte clair.
 */
public class AdminLoginView {

    private static final Logger logger = LogManager.getLogger(AdminLoginView.class);

    @FXML private TextField emailField;
    @FXML private TextField privateKeyPathField;
    @FXML private PasswordField keystorePasswordField;
    @FXML private Button browseButton;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    @FXML private ProgressBar progressBar;

    private File selectedKeystoreFile;

    private static final String KEYSTORE_FILENAME = "admin-keystore.p12";

    @FXML
    public void initialize() {
        progressBar.setVisible(false);
        autoDetectKeystore();
    }

    /**
     * Cherche automatiquement le fichier keystore admin dans les emplacements courants.
     * Ordre de recherche :
     *   1. security/admin-keystore.p12 (relatif au répertoire de travail)
     *   2. Dossier utilisateur ~/admin-keystore.p12
     */
    private void autoDetectKeystore() {
        String[] searchPaths = {
            "security" + File.separator + KEYSTORE_FILENAME,
            System.getProperty("user.home") + File.separator + KEYSTORE_FILENAME
        };

        for (String path : searchPaths) {
            File candidate = new File(path);
            if (candidate.exists() && candidate.isFile()) {
                selectedKeystoreFile = candidate;
                privateKeyPathField.setText(candidate.getAbsolutePath());
                messageLabel.setText("Keystore détecté automatiquement.");
                messageLabel.setStyle("-fx-text-fill: green;");
                logger.info("Keystore admin auto-détecté : " + candidate.getAbsolutePath());
                return;
            }
        }
        logger.info("Aucun keystore auto-détecté. L'admin devra le sélectionner manuellement.");
    }

    @FXML
    private void handleBrowsePrivateKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le Keystore Admin (.p12)");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Keystore PKCS12 (*.p12)", "*.p12"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) emailField.getScene().getWindow();
        File selected = fileChooser.showOpenDialog(stage);

        if (selected != null) {
            selectedKeystoreFile = selected;
            privateKeyPathField.setText(selected.getAbsolutePath());
            messageLabel.setText("Fichier keystore sélectionné.");
            messageLabel.setStyle("-fx-text-fill: green;");
        }
    }

    @FXML
    private void handleAdminLogin() {
        String email = emailField.getText().trim();
        String keystorePassword = keystorePasswordField.getText();

        if (email.isEmpty()) {
            afficherErreur("Veuillez entrer votre email admin");
            return;
        }

        if (selectedKeystoreFile == null) {
            afficherErreur("Veuillez sélectionner votre fichier keystore (.p12)");
            return;
        }

        if (keystorePassword.isEmpty()) {
            afficherErreur("Veuillez entrer le mot de passe du keystore");
            return;
        }

        // Load the private key from the PKCS12 keystore
        PrivateKey privateKey;
        try {
            KeyStoreManager ksm = new KeyStoreManager(selectedKeystoreFile, keystorePassword);
            privateKey = ksm.getPrivateKey(email, keystorePassword);
            if (privateKey == null) {
                afficherErreur("Aucune clé trouvée pour l'alias : " + email);
                return;
            }
        } catch (Exception e) {
            logger.error("Erreur chargement keystore : " + e.getMessage());
            afficherErreur("Mot de passe incorrect ou keystore invalide.");
            return;
        }

        loginButton.setDisable(true);
        progressBar.setVisible(true);
        messageLabel.setText("Authentification en cours...");
        messageLabel.setStyle("-fx-text-fill: #1a73e8;");

        final PrivateKey finalKey = privateKey;

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

                // Sign the challenge with the private key from the keystore
                String signatureBase64 = RSASigner.signToBase64(challenge, finalKey);
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
