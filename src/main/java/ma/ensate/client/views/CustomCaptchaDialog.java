package ma.ensate.client.views;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.protocol.Request;
import ma.ensate.protocol.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Dialogue modal de CAPTCHA personnalisé de niveau production.
 * Entièrement écrit en JavaFX natif avec un design sombre premium cohérent avec ChriOnline.
 * Ce dialogue ne se ferme pas lors d'échecs de validation (mot de passe ou captcha erronés) ;
 * il affiche l'erreur en direct et rafraîchit automatiquement l'image.
 */
public class CustomCaptchaDialog {

    private static final Logger logger = LogManager.getLogger(CustomCaptchaDialog.class);

    private final CaptchaSubmitListener submitListener;

    private String captchaId;
    private String captchaSessionToken;
    private boolean successful = false;

    /**
     * Interface de rappel pour valider l'action de manière atomique sur le serveur.
     */
    public interface CaptchaSubmitListener {
        Response onSubmit(String captchaId, String captchaInput, String captchaSessionToken) throws Exception;
    }

    public CustomCaptchaDialog(CaptchaSubmitListener submitListener) {
        this.submitListener = submitListener;
    }

    /**
     * Affiche la boîte de dialogue modale et lance la validation.
     * @param owner La fenêtre parente JavaFX.
     * @return true si l'action soumise via onSubmit s'est déroulée avec succès sur le serveur.
     */
    public boolean showAndWait(Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Sécurité — Vérification");
        stage.setResizable(false);

        // Layout principal
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);
        
        // Thème sombre haut de gamme (matching ChriOnline)
        layout.setStyle(
            "-fx-background-color: #0f172a; " + // Deep slate background
            "-fx-border-color: #334155; " +    // Subtle slate borders
            "-fx-border-width: 1; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12;"
        );

        Label titleLabel = new Label("SÉCURITÉ ANTI-BOT");
        titleLabel.setStyle("-fx-font-family: 'Outfit', 'Inter', 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-letter-spacing: 1px;");

        Label descLabel = new Label("Veuillez saisir les caractères déformés ci-dessous pour continuer :");
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-alignment: center;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(260);

        // Composants CAPTCHA
        ImageView captchaImageView = new ImageView();
        captchaImageView.setFitWidth(220);
        captchaImageView.setFitHeight(65);
        captchaImageView.setPreserveRatio(true);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle(
            "-fx-background-color: #1e293b; " +
            "-fx-text-fill: #38bdf8; " +
            "-fx-font-size: 16px; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #334155; " +
            "-fx-border-radius: 8; " +
            "-fx-padding: 8 12;"
        );
        
        // Effets sur le survol du bouton de rafraîchissement
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-color: #38bdf8; -fx-border-radius: 8; -fx-padding: 8 12;"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 8 12;"));

        HBox captchaContainer = new HBox(12, captchaImageView, refreshBtn);
        captchaContainer.setAlignment(Pos.CENTER);

        TextField captchaField = new TextField();
        captchaField.setPromptText("Saisir le code ici (insensible à la casse)");
        captchaField.setPrefHeight(45);
        captchaField.setStyle(
            "-fx-background-color: #1e293b; " +
            "-fx-text-fill: #f8fafc; " +
            "-fx-border-color: #334155; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-font-size: 13px; " +
            "-fx-alignment: center;"
        );
        captchaField.setOnKeyReleased(e -> {
            // Styliser la bordure en bleu au focus/frappe
            captchaField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-border-color: #38bdf8; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px; -fx-alignment: center;");
        });

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(260);

        Button submitBtn = new Button("VALIDER");
        submitBtn.setStyle(
            "-fx-background-color: #0284c7; " + // Deep premium blue
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 12 24; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnMouseEntered(e -> submitBtn.setStyle("-fx-background-color: #0369a1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"));
        submitBtn.setOnMouseExited(e -> submitBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"));

        Button cancelBtn = new Button("ANNULER");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 12px; -fx-cursor: hand;"));

        // Action de rafraîchissement
        refreshBtn.setOnAction(e -> loadNewCaptcha(captchaImageView, errorLabel, captchaField));
        
        // Charger le premier captcha au démarrage
        loadNewCaptcha(captchaImageView, errorLabel, captchaField);

        submitBtn.setOnAction(e -> {
            String input = captchaField.getText().trim();
            if (input.isEmpty()) {
                errorLabel.setText("Veuillez saisir le code.");
                return;
            }

            submitBtn.setDisable(true);
            refreshBtn.setDisable(true);
            errorLabel.setText("Vérification et authentification...");
            errorLabel.setStyle("-fx-text-fill: #38bdf8;");

            new Thread(() -> {
                try {
                    // Appel du callback d'authentification atomique sur le serveur
                    Response response = submitListener.onSubmit(captchaId, input, captchaSessionToken);

                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        refreshBtn.setDisable(false);
                        
                        if (response.isSuccess()) {
                            successful = true;
                            stage.close();
                        } else {
                            // En cas d'échec (identifiant faux OU mauvais captcha), on reste ouvert !
                            errorLabel.setText(response.getMessage());
                            errorLabel.setStyle("-fx-text-fill: #f43f5e;");
                            
                            // Réinitialiser le champ de saisie et forcer un rafraîchissement de captcha obligatoire
                            captchaField.clear();
                            loadNewCaptcha(captchaImageView, null, captchaField);
                        }
                    });
                } catch (Exception ex) {
                    logger.error("Erreur soumission formulaire avec CAPTCHA : " + ex.getMessage());
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        refreshBtn.setDisable(false);
                        errorLabel.setText("Erreur réseau. Impossible de contacter le serveur.");
                        errorLabel.setStyle("-fx-text-fill: #f43f5e;");
                    });
                }
            }).start();
        });

        cancelBtn.setOnAction(e -> stage.close());

        layout.getChildren().addAll(titleLabel, descLabel, captchaContainer, captchaField, errorLabel, submitBtn, cancelBtn);

        Scene scene = new Scene(layout, 330, 380);
        stage.setScene(scene);
        stage.showAndWait();

        return successful;
    }

    /**
     * Charge asynchronement un nouveau CAPTCHA depuis l'endpoint GET_CAPTCHA_NEW.
     */
    private void loadNewCaptcha(ImageView imageView, Label errorLabel, TextField captchaField) {
        new Thread(() -> {
            try {
                Request req = new Request("GET_CAPTCHA_NEW", null);
                Response resp = ClientTCP.getInstance().envoyerRequete(req);
                
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        Object[] data = (Object[]) resp.getData();
                        this.captchaId = (String) data[0];
                        String base64 = (String) data[1];
                        this.captchaSessionToken = (String) data[2];

                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        imageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                        
                        if (errorLabel != null) {
                            errorLabel.setText("");
                        }
                        if (captchaField != null) {
                            captchaField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-border-color: #334155; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px; -fx-alignment: center;");
                        }
                    } else {
                        if (errorLabel != null) {
                            errorLabel.setText(resp.getMessage());
                            errorLabel.setStyle("-fx-text-fill: #f43f5e;");
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Erreur lors du chargement du CAPTCHA : " + e.getMessage());
                Platform.runLater(() -> {
                    if (errorLabel != null) {
                        errorLabel.setText("Erreur de connexion lors du chargement du CAPTCHA.");
                        errorLabel.setStyle("-fx-text-fill: #f43f5e;");
                    }
                });
            }
        }).start();
    }
}
