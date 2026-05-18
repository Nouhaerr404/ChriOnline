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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;

/**
 * Vue de connexion client de ChriOnline.
 * Utilise Google reCAPTCHA v2 modal pour la sécurité anti-bot.
 */
public class LoginView {

    private static final Logger logger = LogManager.getLogger(LoginView.class);

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisibleField;
    @FXML private Label         messageLabel;
    @FXML private Button        loginButton;
    @FXML private Button        togglePasswordBtn;

    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        // reCAPTCHA est modal, aucune initialisation nécessaire au démarrage de la vue
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = getPasswordValue();

        if (email.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs !");
            return;
        }

        Stage owner = (Stage) emailField.getScene().getWindow();
        
        java.util.concurrent.atomic.AtomicReference<Response> successResponse = new java.util.concurrent.atomic.AtomicReference<>();

        CustomCaptchaDialog dialog = new CustomCaptchaDialog((captchaId, captchaInput, captchaSessionToken) -> {
            Object[] payload = {email, password, captchaId, captchaInput, captchaSessionToken};
            Request request = new Request("LOGIN", payload);
            Response response = ClientTCP.getInstance().envoyerRequete(request);
            if (response.isSuccess()) {
                successResponse.set(response);
            }
            return response;
        });

        loginButton.setDisable(true);
        messageLabel.setText("Attente de validation CAPTCHA...");
        messageLabel.setStyle("-fx-text-fill: #1E293B;");

        boolean ok = dialog.showAndWait(owner);

        loginButton.setDisable(false);

        if (ok) {
            Response response = successResponse.get();
            if (response != null) {
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
                    logger.error("Erreur enregistrement port UDP : " + e.getMessage());
                }
                logger.info("Login réussi : " + email);
                ouvrirPagePrincipale();
            }
        } else {
            messageLabel.setText("");
        }
    }

    @FXML
    private void allerAdminLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/admin_login.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Connexion Admin");
        } catch (Exception e) {
            logger.error("Erreur navigation vers admin login : "
                    + e.getMessage());
        }
    }

    @FXML
    private void allerAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/landing.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Premium E-Commerce");
        } catch (Exception e) {
            logger.error("Erreur navigation vers accueil : " + e.getMessage());
        }
    }

    @FXML
    public void allerInscription(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/register.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("ChriOnline — Inscription");
        } catch (Exception e) {
            logger.error("Erreur navigation vers inscription : " + e.getMessage());
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

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);
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
}
