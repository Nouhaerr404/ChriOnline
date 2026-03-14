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
            // TODO : remplacer par la vraie page principale
            // quand Personne 2 aura créé ProduitsView
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("ChriOnline — Bienvenue "
                    + SessionManager.getInstance().getNomUtilisateur());
        } catch (Exception e) {
            // Si la page produits n'existe pas encore
            // afficher juste un message de succès
            messageLabel.setText(" Connecté en tant que : "
                    + SessionManager.getInstance().getNomUtilisateur());
            messageLabel.setStyle("-fx-text-fill: green;");
            logger.info("Page produits pas encore disponible.");
        }
    }

    // =============================================
    // UTILITAIRES
    // =============================================
    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red;");
    }
}