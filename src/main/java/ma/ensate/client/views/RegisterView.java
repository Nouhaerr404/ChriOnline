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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegisterView {

    private static final Logger logger = LogManager.getLogger(RegisterView.class);

    @FXML private TextField     nomField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmMdpField;
    @FXML private TextField     passwordVisibleField;
    @FXML private TextField     adresseField;
    @FXML private TextField     telField;
    @FXML private Label         messageLabel;
    @FXML private Button        registerButton;
    @FXML private Button        togglePasswordBtn;
    @FXML private ImageView     captchaImageView;
    @FXML private TextField     captchaField;
    @FXML private Button        refreshCaptchaBtn;

    private boolean passwordVisible = false;
    private String captchaId;

    @FXML
    public void initialize() {
        chargerCaptcha();
    }

    @FXML
    private void handleRegister() {
        String nom      = nomField.getText().trim();
        String email    = emailField.getText().trim();
        String password = getPasswordValue();
        String adresse  = adresseField.getText().trim();
        String tel      = telField.getText().trim();
        String captchaAnswer = captchaField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || password.isEmpty() || captchaAnswer.isEmpty()) {
            afficherErreur("Nom, email, mot de passe et captcha sont obligatoires !");
            return;
        }

        if (captchaId == null || captchaId.isEmpty()) {
            afficherErreur("Captcha indisponible. Rafraichissez.");
            return;
        }

        if (password.length() < 6 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[1-9].*") || !password.matches(".*[@&#%!].*")) {
            afficherErreur("Le mot de passe doit contenir au moins 6 caracteres et il doit contenir au moins une lettre majuscule, une lettre miniscule, une chiffre  et des caracteres speciaux comme ( @ & # % !) ");
            return;
        }

        registerButton.setDisable(true);
        refreshCaptchaBtn.setDisable(true);
        afficherInfo("Inscription en cours...");

        new Thread(() -> {
            try {
                Client client = new Client(nom, email, password, adresse, tel);
                Object[] payload = {client, captchaId, captchaAnswer};
                Request request = new Request("REGISTER", payload);

                Response response = ClientTCP.getInstance()
                        .envoyerRequete(request);

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    refreshCaptchaBtn.setDisable(false);

                    if (response.isSuccess()) {
                        afficherSucces(" Inscription reussie ! Redirection...");
                        logger.info(" Inscription reussie : " + email);
                        
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(this::allerLogin);
                            } catch (InterruptedException ignored) {}
                        }).start();

                    } else {
                        afficherErreur(response.getMessage());
                        chargerCaptcha();
                        captchaField.clear();
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

    @FXML

    public void allerLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/login.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) nomField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Connexion");
        } catch (Exception e) {
            logger.error("Erreur navigation vers login : " + e.getMessage());
        }
    }

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
        messageLabel.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 12px;");
    }

    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            togglePasswordBtn.setText("Masquer");
            return;
        }

        passwordField.setText(passwordVisibleField.getText());
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        togglePasswordBtn.setText("Voir");
    }

    private String getPasswordValue() {
        return passwordVisible ? passwordVisibleField.getText() : passwordField.getText();
    }

    @FXML
    private void handleRefreshCaptcha() {
        chargerCaptcha();
    }

    private void chargerCaptcha() {
        if (refreshCaptchaBtn != null) refreshCaptchaBtn.setDisable(true);
        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance().envoyerRequete(new Request("GET_CAPTCHA"));
                Platform.runLater(() -> {
                    if (refreshCaptchaBtn != null) refreshCaptchaBtn.setDisable(false);
                    if (!response.isSuccess() || !(response.getData() instanceof String[] data) || data.length < 2) {
                        captchaId = null;
                        logger.error("Echec chargement captcha : response.isSuccess=" + response.isSuccess());
                        afficherErreur("Impossible de charger le captcha.");
                        return;
                    }
                    captchaId = data[0];
                    logger.info("Captcha recu - ID: " + captchaId + " | Base64 Length: " + (data[1] != null ? data[1].length() : 0));

                    if (data[1] == null || data[1].isBlank()) {
                        logger.error("Données image captcha vides !");
                        afficherErreur("Erreur image captcha.");
                        return;
                    }

                    // Décoder le base64 → Image JavaFX
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(data[1].trim());
                        Image fxImage = new Image(new ByteArrayInputStream(imageBytes));
                        if (fxImage.isError()) {
                            logger.error("Erreur chargement Image FX: " + fxImage.getException());
                        }
                        captchaImageView.setImage(fxImage);
                    } catch (Exception e) {
                        logger.error("Erreur decodage captcha image : " + e.getMessage());
                        afficherErreur("Erreur affichage captcha.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    captchaId = null;
                    if (refreshCaptchaBtn != null) refreshCaptchaBtn.setDisable(false);
                    afficherErreur("Impossible de charger le captcha.");
                });
            }
        }).start();
    }
}
