package ma.ensate.client.views;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ma.ensate.client.network.ClientTCP;
import ma.ensate.models.Client;
import ma.ensate.models.Commande;
import ma.ensate.models.StatutCommande;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour tester vos vues SANS avoir besoin du panier ou du serveur.
 * Pour lancer : modifiez le pom.xml ou créez une configuration de lancement.
 */
public class DebugLauncher extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #F5F5F7;");

        Button btnHistorique = creerBouton("Tester: Historique des Commandes");
        btnHistorique.setOnAction(e -> testHistorique());

        Button btnCommande = creerBouton("Tester: Validation Commande (Récap)");
        btnCommande.setOnAction(e -> testValidation());

        Button btnPaiement = creerBouton("Tester: Page de Paiement");
        btnPaiement.setOnAction(e -> testPaiement());

        root.getChildren().addAll(btnHistorique, btnCommande, btnPaiement);

        Scene scene = new Scene(root, 400, 400);
        primaryStage.setTitle("Debug App Views");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button creerBouton(String texte) {
        Button b = new Button(texte);
        b.setStyle("-fx-background-color: #0071E3; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    // --- TESTS INDIVIDUELS ---

    private void testHistorique() {
        // Lance l'historique (Note: Nécessite que le serveur soit lancé si on veut de vraies données, 
        // sinon la vue affichera "Erreur de connexion")
        new HistoriqueView(primaryStage, ClientTCP.getInstance(), 1, "mock-token").afficher();
    }

    private void testValidation() {
        // Mock de données pour tester la vue sans passer par le panier
        List<PanierView.LigneTableau> mockArticles = new ArrayList<>();
        mockArticles.add(new PanierView.LigneTableau(1, "iPhone 15 Pro", 12000.0, 1, 12000.0));
        mockArticles.add(new PanierView.LigneTableau(2, "AirPods Pro 2", 2500.0, 1, 2500.0));

        new CommandeView(primaryStage, ClientTCP.getInstance(), 1, "mock-token", 14500.0, mockArticles).afficher();
    }

    private void testPaiement() {
        // Mock d'une commande pour tester la page de paiement
        Client mockClient = new Client();
        mockClient.setId(1);
        
        Commande mockCmd = new Commande(mockClient);
        mockCmd.setPrixAPayer(14500.0);
        mockCmd.setStatut(StatutCommande.EN_ATTENTE);
        
        new PaiementView(primaryStage, ClientTCP.getInstance(), 1, "mock-token", mockCmd).afficher();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
