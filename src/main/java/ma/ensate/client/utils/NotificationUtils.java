package ma.ensate.client.utils;

import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationUtils {

    /**
     * Affiche un popup Neon Noir centré sur le Stage.
     * Nécessite que le root de la scène soit un StackPane.
     */
    public static void showNeonPopup(Stage stage, String title, String message, boolean success) {
        if (stage == null || stage.getScene() == null) return;
        
        Platform.runLater(() -> {
            try {
                Parent rootParent = stage.getScene().getRoot();
                if (!(rootParent instanceof StackPane)) {
                    // Fallback
                    showToast(stage, title + ": " + message);
                    return;
                }
                
                StackPane root = (StackPane) rootParent;
                
                VBox overlay = new VBox();
                overlay.setAlignment(Pos.CENTER);
                overlay.setStyle("-fx-background-color: rgba(7, 18, 36, 0.85);");
                
                VBox card = new VBox(25);
                card.setAlignment(Pos.CENTER);
                card.setMaxWidth(450);
                card.setPadding(new javafx.geometry.Insets(40));
                card.setStyle("-fx-background-color: #0d1b31; -fx-background-radius: 25; " +
                             "-fx-border-color: " + (success ? "#5cff90" : "#ff5da9") + "; -fx-border-width: 2; " +
                             "-fx-effect: dropshadow(three-pass-box, " + (success ? "rgba(92,255,144,0.3)" : "rgba(255,93,169,0.3)") + ", 20, 0, 0, 0);");
                
                Label lblTitle = new Label(title.toUpperCase());
                lblTitle.setStyle("-fx-text-fill: " + (success ? "#5cff90" : "#ff5da9") + "; -fx-font-size: 20px; -fx-font-weight: bold;");
                
                Label lblMsg = new Label(message);
                lblMsg.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15px;");
                lblMsg.setWrapText(true);
                lblMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                
                Button btn = new Button("COMPRIS");
                btn.setCursor(javafx.scene.Cursor.HAND);
                btn.setStyle("-fx-background-color: " + (success ? "#5cff90" : "#ff5da9") + "; -fx-text-fill: #071224; " +
                            "-fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 35;");
                btn.setOnAction(e -> root.getChildren().remove(overlay));
                
                card.getChildren().addAll(lblTitle, lblMsg, btn);
                overlay.getChildren().add(card);
                root.getChildren().add(overlay);
                
            } catch (Exception e) {
                showToast(stage, message);
            }
        });
    }

    public static void showToast(Stage stage, String message) {
        if (stage == null || !stage.isShowing()) return;

        Platform.runLater(() -> {
            Popup popup = new Popup();
            
            Label label = new Label(message);
            label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            HBox toastRoot = new HBox(label);
            toastRoot.setAlignment(Pos.CENTER);
            toastRoot.setPrefHeight(60);
            toastRoot.setMinWidth(300);
            toastRoot.setStyle("-fx-background-color: #0d1b31; " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-color: #ff5da9; " +
                            "-fx-border-width: 0 0 0 5; " +
                            "-fx-padding: 15; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 5);");

            popup.getContent().add(toastRoot);
            popup.setAutoHide(true);

            popup.setOnShown(e -> {
                popup.setX(stage.getX() + (stage.getWidth() - toastRoot.getWidth()) / 2);
                popup.setY(stage.getY() + stage.getHeight() - 100);
            });

            toastRoot.setOpacity(0);
            toastRoot.setTranslateY(20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), toastRoot);
            slideUp.setFromY(20);
            slideUp.setToY(0);

            fadeIn.play();
            slideUp.play();

            popup.show(stage);

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastRoot);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> popup.hide());
                fadeOut.play();
            });
            delay.play();
        });
    }
}
