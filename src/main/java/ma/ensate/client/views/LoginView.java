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

public class LoginView {

    private static final Logger logger = LogManager.getLogger(LoginView.class);

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;
    @FXML private Button        loginButton;

    // =============================================
    // HANDLE LOGIN
    // =============================================
    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation côté client
        if (email.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs !");
            return;
        }

        // Désactiver le bouton pendant la requête
        loginButton.setDisable(true);
        messageLabel.setText("Connexion en cours...");
        messageLabel.setStyle("-fx-text-fill: #1a73e8;");

        // Envoyer dans un thread séparé
        // pour ne pas bloquer l'interface JavaFX
        new Thread(() -> {
            try {
                // Créer la requête
                String[] credentials = {email, password};
                Request request = new Request("LOGIN", credentials);

                // Envoyer au serveur
                Response response = ClientTCP.getInstance()
                        .envoyerRequete(request);

                // Retourner sur le thread JavaFX
                Platform.runLater(() -> {
                    loginButton.setDisable(false);

                    if (response.isSuccess()) {
                        // Sauvegarder la session
                        Utilisateur u = (Utilisateur) response.getData();
                        SessionManager.getInstance().setUtilisateur(u);
                        logger.info(" Login réussi : " + email);

                        // Ouvrir la page principale
                        ouvrirPagePrincipale();

                    } else {
                        afficherErreur(response.getMessage());
                        if (response.getMessage().contains("bloqué")) {
                            emailField.setDisable(true);
                            passwordField.setDisable(true);
                            loginButton.setDisable(true);
                            messageLabel.setText(
                                    " Réessayez après 5 minutes.");
                            messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                            // Débloquer automatiquement après 5 minutes
                            new Thread(() -> {
                                try {
                                    Thread.sleep(5 * 60 * 1000); // 5 minutes
                                    Platform.runLater(() -> {
                                        emailField.setDisable(false);
                                        passwordField.setDisable(false);
                                        loginButton.setDisable(false);
                                        emailField.clear();
                                        passwordField.clear();
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
                    afficherErreur("Impossible de contacter le serveur.");
                    logger.error("Erreur login : " + e.getMessage());
                });
            }
        }).start();
    }

    // =============================================
    // ALLER À L'INSCRIPTION
    // =============================================
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

    // =============================================
    // OUVRIR LA PAGE PRINCIPALE
    // =============================================
    private void ouvrirPagePrincipale() {
        try {
            String target = SessionManager.getInstance().estAdmin()
                ? "/ma/ensate/fxml/admin_produits.fxml"
                : "/ma/ensate/fxml/produits.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("ChriOnline — " + SessionManager.getInstance().getNomUtilisateur());
        } catch (Exception e) {
            // produits.fxml pas encore disponible (P2 n'a pas pushé)
            // navigation temporaire vers PanierView pour les tests
            logger.info("produits.fxml indisponible — ouverture PanierView (temp)");
            Utilisateur u = SessionManager.getInstance().getUtilisateur();
            Stage stage   = (Stage) emailField.getScene().getWindow();
            new PanierView(stage, ClientTCP.getInstance(), u.getId(), u.getSessionToken()).afficher();
        }
    }
    // UTILITAIRES
    // =============================================
    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red;");
    }
}