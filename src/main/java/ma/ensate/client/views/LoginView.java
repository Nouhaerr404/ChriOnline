package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.client.network.SessionManager;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class LoginView {

    private static final Logger logger = LogManager.getLogger(LoginView.class);

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisibleField;
    @FXML private TextField     captchaField;
    @FXML private ImageView     captchaImageView;
    @FXML private Label         messageLabel;
    @FXML private Button        loginButton;
    @FXML private Button        togglePasswordBtn;
    @FXML private Button        refreshCaptchaBtn;

    private boolean passwordVisible = false;
    private String captchaId;

    @FXML
    public void initialize() {
        chargerCaptcha();
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = getPasswordValue();
        String captchaAnswer = captchaField.getText().trim();

        if (email.isEmpty() || password.isEmpty() || captchaAnswer.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs !");
            return;
        }
        if (captchaId == null || captchaId.isBlank()) {
            afficherErreur("Captcha indisponible. Rafraichissez puis reessayez.");
            return;
        }

        loginButton.setDisable(true);
        refreshCaptchaBtn.setDisable(true);
        messageLabel.setText("Connexion en cours...");
        messageLabel.setStyle("-fx-text-fill: #1E293B;");

        new Thread(() -> {
            try {

                Object[] payload = {email, password, captchaId, captchaAnswer};
                Request request = new Request("LOGIN", payload);

                Response response = ClientTCP.getInstance()
                        .envoyerRequete(request);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    refreshCaptchaBtn.setDisable(false);

                    if (response.isSuccess()) {

                        if ("REQUIRES_2FA".equals(response.getMessage())) {
                            Object[] twoFaPayload = (Object[]) response.getData();
                            int    userId    = (int)    twoFaPayload[0];
                            String userEmail = (String) twoFaPayload[1];
                            afficherDialogOtp(userId, userEmail);
                            return;
                        }

                        Utilisateur u = (Utilisateur) response.getData();
                        SessionManager.getInstance().setUtilisateur(u);
                        int udpPort = SessionManager.getInstance().getUdpPort();
                        try {
                            ClientTCP.getInstance().envoyerRequeteSecurisee(
                                    "REGISTER_UDP_PORT",
                                    new Object[]{u.getId(), udpPort});
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        logger.info(" Login réussi : " + email);

                        ouvrirPagePrincipale();

                    } else {
                        afficherErreur(response.getMessage());
                        chargerCaptcha();
                        captchaField.clear();
                        if (response.getMessage().contains("bloqué")) {
                            emailField.setDisable(true);
                            setPasswordInputsDisabled(true);
                            captchaField.setDisable(true);
                            refreshCaptchaBtn.setDisable(true);
                            loginButton.setDisable(true);
                            messageLabel.setText(
                                    " Réessayez après 5 minutes.");
                            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                            new Thread(() -> {
                                try {
                                    Thread.sleep(5 * 60 * 1000);
                                    Platform.runLater(() -> {
                                        emailField.setDisable(false);
                                        setPasswordInputsDisabled(false);
                                        captchaField.setDisable(false);
                                        refreshCaptchaBtn.setDisable(false);
                                        loginButton.setDisable(false);
                                        emailField.clear();
                                        clearPasswordFields();
                                        captchaField.clear();
                                        chargerCaptcha();
                                        messageLabel.setText(
                                                "Vous pouvez réessayer maintenant.");
                                        messageLabel.setStyle("-fx-text-fill: green;");
                                    });
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    refreshCaptchaBtn.setDisable(false);
                    afficherErreur("Impossible de contacter le serveur.");
                    logger.error("Erreur login : " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void allerInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/register.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 600));
            stage.setTitle("ChriOnline — Inscription");
        } catch (Exception e) {
            logger.error("Erreur navigation vers inscription : "
                    + e.getMessage());
        }
    }

    private void afficherDialogOtp(int userId, String userEmail) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Vérification 2FA");
        dialog.setHeaderText("Un code de vérification a été envoyé à :\n" + userEmail);
        dialog.setContentText("Entrez le code à 6 chiffres :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            loginButton.setDisable(false);
            afficherErreur("Vérification annulée.");
            return;
        }

        String code = result.get().trim();
        loginButton.setDisable(true);
        messageLabel.setText("Vérification en cours...");
        messageLabel.setStyle("-fx-text-fill: #1E293B;");

        new Thread(() -> {
            try {
                Object[] params = {userId, code};
                Response response = ClientTCP.getInstance()
                        .envoyerRequete(new Request("VERIFY_2FA", params));

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if (response.isSuccess()) {
                        Utilisateur u = (Utilisateur) response.getData();
                        SessionManager.getInstance().setUtilisateur(u);
                        try {
                            int udpPort = SessionManager.getInstance().getUdpPort();
                            ClientTCP.getInstance().envoyerRequeteSecurisee(
                                    "REGISTER_UDP_PORT", new Object[]{u.getId(), udpPort});
                        } catch (Exception e) {
                            logger.warn("Erreur enregistrement port UDP : " + e.getMessage());
                        }
                        logger.info("2FA validé, connexion réussie : userId=" + userId);
                        ouvrirPagePrincipale();
                    } else {
                        afficherErreur(response.getMessage());
                        afficherDialogOtp(userId, userEmail);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    afficherErreur("Impossible de contacter le serveur.");
                });
                logger.error("Erreur VERIFY_2FA : " + e.getMessage());
            }
        }).start();
    }

    private void ouvrirPagePrincipale() {        try {
            String target = SessionManager.getInstance().estAdmin()
                ? "/ma/ensate/fxml/admin_produits.fxml"
                : "/ma/ensate/fxml/produits.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
            stage.setTitle("ChriOnline — " + SessionManager.getInstance().getNomUtilisateur());
        } catch (Exception e) {
            logger.info("produits.fxml indisponible — ouverture PanierView (temp)");
            Utilisateur u = SessionManager.getInstance().getUtilisateur();
            Stage stage   = (Stage) emailField.getScene().getWindow();
            new PanierView(stage, ClientTCP.getInstance(), u.getId(), u.getSessionToken()).afficher();
        }
    }

    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red;");
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

    private void clearPasswordFields() {
        passwordField.clear();
        passwordVisibleField.clear();
    }

    private void setPasswordInputsDisabled(boolean disabled) {
        passwordField.setDisable(disabled);
        passwordVisibleField.setDisable(disabled);
        togglePasswordBtn.setDisable(disabled);
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
