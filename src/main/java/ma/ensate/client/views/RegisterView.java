package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Client;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Vue d'inscription client de ChriOnline.
 * Utilise Google reCAPTCHA v2 modal pour la sécurité anti-bot.
 */
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

    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        // reCAPTCHA est modal, aucune initialisation nécessaire au démarrage de la vue
    }

    @FXML
    private void handleRegister() {
        String nom      = nomField.getText().trim();
        String email    = emailField.getText().trim();
        String password = getPasswordValue();
        String adresse  = adresseField.getText().trim();
        String tel      = telField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            afficherErreur("Nom, email et mot de passe sont obligatoires !");
            return;
        }

        if (password.length() < 6 || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*") || !password.matches(".*[@&#%!].*")) {
            afficherErreur("Le mot de passe doit contenir au moins 6 caractères, une lettre majuscule, une minuscule, un chiffre et un caractère spécial (@, &, #, %, !).");
            return;
        }

        String confirmPassword = confirmMdpField.getText();
        if (!password.equals(confirmPassword)) {
            afficherErreur("Les mots de passe ne correspondent pas !");
            return;
        }

        if (!tel.isEmpty() && !tel.matches("^[0-9+]{8,15}$")) {
            afficherErreur("Numéro de téléphone invalide (8 à 15 chiffres, chiffres et '+' uniquement).");
            return;
        }

        Stage owner = (Stage) nomField.getScene().getWindow();

        java.util.concurrent.atomic.AtomicReference<Response> successResponse = new java.util.concurrent.atomic.AtomicReference<>();

        CustomCaptchaDialog dialog = new CustomCaptchaDialog((captchaId, captchaInput, captchaSessionToken) -> {
            ma.ensate.models.Client client = new ma.ensate.models.Client(nom, email, password, adresse, tel);
            Object[] payload = {client, captchaId, captchaInput, captchaSessionToken};
            Request request = new Request("REGISTER", payload);
            Response response = ClientTCP.getInstance().envoyerRequete(request);
            if (response.isSuccess()) {
                successResponse.set(response);
            }
            return response;
        });

        registerButton.setDisable(true);
        afficherInfo("Attente de validation CAPTCHA...");

        boolean ok = dialog.showAndWait(owner);

        registerButton.setDisable(false);

        if (ok) {
            Response response = successResponse.get();
            if (response != null) {
                afficherSucces("Inscription réussie ! Redirection...");
                logger.info("Inscription réussie : " + email);
                
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(this::allerLogin);
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        } else {
            afficherInfo("Tous les champs sont obligatoires");
        }
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
}
