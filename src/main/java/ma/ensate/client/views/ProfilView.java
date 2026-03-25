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
import ma.ensate.models.Client;
import ma.ensate.models.Utilisateur;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProfilView {

    private static final Logger logger = LogManager.getLogger(ProfilView.class);

    @FXML private TextField     nomField;
    @FXML private TextField     emailField;
    @FXML private TextField     adresseField;
    @FXML private TextField     telField;
    @FXML private Label         statutLabel;
    @FXML private Label         messageLabel;
    @FXML private PasswordField ancienPasswordField;
    @FXML private PasswordField nouveauPasswordField;
    @FXML private Label         passwordMessageLabel;
    @FXML private TextField ancienPasswordVisible;
    @FXML private TextField nouveauPasswordVisible;
    @FXML private Button    toggleAncienBtn;
    @FXML private Button    toggleNouveauBtn;


    private boolean ancienVisible  = false;
    private boolean nouveauVisible = false;

    @FXML
    public void initialize() {
        chargerProfil();
    }

    private void chargerProfil() {
        Utilisateur u = SessionManager.getInstance().getUtilisateur();
        if (u == null) return;

        // Charger depuis SessionManager d'abord
        nomField.setText(u.getNom() != null ? u.getNom() : "");
        emailField.setText(u.getEmail() != null ? u.getEmail() : "");

        if (u instanceof Client) {
            Client c = (Client) u;
            adresseField.setText(c.getAdresse() != null ? c.getAdresse() : "");
            telField.setText(c.getTel() != null ? c.getTel() : "");
        }

        afficherStatut(u);

        new Thread(() -> {
            try {
                Response response = ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("GET_PROFIL", u.getId());

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Utilisateur profil = (Utilisateur) response.getData();
                        nomField.setText(profil.getNom() != null
                                ? profil.getNom() : "");
                        emailField.setText(profil.getEmail() != null
                                ? profil.getEmail() : "");
                        if (profil instanceof Client) {
                            Client c = (Client) profil;
                            adresseField.setText(c.getAdresse() != null
                                    ? c.getAdresse() : "");
                            telField.setText(c.getTel() != null
                                    ? c.getTel() : "");
                        }
                        afficherStatut(profil);
                    }
                });
            } catch (Exception e) {
                logger.error("Erreur chargement profil : " + e.getMessage());
            }
        }).start();
    }

    private void afficherStatut(Utilisateur u) {
        if (u.getStatut() != null && u.getStatut().equals("SUSPENDU")) {
            statutLabel.setText(" Suspendu");
            statutLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            statutLabel.setText(" Actif");
            statutLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleUpdateProfil() {
        String nom     = nomField.getText().trim();
        String adresse = adresseField.getText().trim();
        String tel     = telField.getText().trim();

        if (nom.isEmpty()) {
            afficherErreur("Le nom est obligatoire !");
            return;
        }

        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance()
                        .getUtilisateur().getId();
                Object[] params = {userId, nom, adresse, tel};

                Response response = ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("UPDATE_PROFIL", params);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        // Mettre à jour SessionManager
                        Utilisateur u = SessionManager.getInstance()
                                .getUtilisateur();
                        u.setNom(nom);
                        if (u instanceof Client) {
                            ((Client) u).setAdresse(adresse);
                            ((Client) u).setTel(tel);
                        }
                        afficherSucces(" Profil mis à jour avec succès !");
                        logger.info("Profil mis à jour");
                    } else {
                        afficherErreur(response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        afficherErreur("Impossible de contacter le serveur."));
                logger.error("Erreur update profil : " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleChangerPassword() {
        String ancien  = ancienVisible
                ? ancienPasswordVisible.getText()
                : ancienPasswordField.getText();

        String nouveau = nouveauVisible
                ? nouveauPasswordVisible.getText()
                : nouveauPasswordField.getText();

        if (ancien.isEmpty() || nouveau.isEmpty()) {
            afficherErreurPassword("Remplissez les deux champs !");
            return;
        }

        if (nouveau.length() < 6) {
            afficherErreurPassword(
                    "Le nouveau mot de passe doit contenir au moins 6 caractères !");
            return;
        }

        new Thread(() -> {
            try {
                int userId = SessionManager.getInstance()
                        .getUtilisateur().getId();
                Object[] params = {userId, ancien, nouveau};

                Response response = ClientTCP.getInstance()
                        .envoyerRequeteSecurisee("CHANGER_PASSWORD", params);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        afficherSuccesPassword(" Mot de passe changé !");
                        ancienPasswordField.clear();
                        nouveauPasswordField.clear();
                        logger.info("Mot de passe changé");
                    } else {
                        afficherErreurPassword(response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        afficherErreurPassword("Impossible de contacter le serveur."));
                logger.error("Erreur changement password : " + e.getMessage());
            }
        }).start();
    }


    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ma/ensate/fxml/produits.fxml"));
            Parent root  = loader.load();
            Stage  stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("ChriOnline — Produits");
        } catch (Exception e) {
            logger.error("Erreur retour : " + e.getMessage());
        }
    }


    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    private void afficherSucces(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: green;");
    }

    private void afficherErreurPassword(String msg) {
        passwordMessageLabel.setText(msg);
        passwordMessageLabel.setStyle("-fx-text-fill: red;");
    }

    private void afficherSuccesPassword(String msg) {
        passwordMessageLabel.setText(msg);
        passwordMessageLabel.setStyle("-fx-text-fill: green;");
    }

    // =============================================
// TOGGLE ANCIEN MOT DE PASSE
// =============================================
    @FXML
    private void toggleAncienPassword() {
        ancienVisible = !ancienVisible;

        if (ancienVisible) {
            // Afficher le texte
            ancienPasswordVisible.setText(ancienPasswordField.getText());
            ancienPasswordField.setVisible(false);
            ancienPasswordField.setManaged(false);
            ancienPasswordVisible.setVisible(true);
            ancienPasswordVisible.setManaged(true);
            toggleAncienBtn.setText("🙈");
        } else {
            // Masquer le texte
            ancienPasswordField.setText(ancienPasswordVisible.getText());
            ancienPasswordVisible.setVisible(false);
            ancienPasswordVisible.setManaged(false);
            ancienPasswordField.setVisible(true);
            ancienPasswordField.setManaged(true);
            toggleAncienBtn.setText("👁");
        }
    }

    @FXML
    private void toggleNouveauPassword() {
        nouveauVisible = !nouveauVisible;

        if (nouveauVisible) {
            nouveauPasswordVisible.setText(nouveauPasswordField.getText());
            nouveauPasswordField.setVisible(false);
            nouveauPasswordField.setManaged(false);
            nouveauPasswordVisible.setVisible(true);
            nouveauPasswordVisible.setManaged(true);
            toggleNouveauBtn.setText("🙈");
        } else {
            nouveauPasswordField.setText(nouveauPasswordVisible.getText());
            nouveauPasswordVisible.setVisible(false);
            nouveauPasswordVisible.setManaged(false);
            nouveauPasswordField.setVisible(true);
            nouveauPasswordField.setManaged(true);
            toggleNouveauBtn.setText("👁");
        }
    }
}