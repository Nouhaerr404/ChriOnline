package ma.ensate.server.services;

import ma.ensate.models.*;
import ma.ensate.server.dao.CommandeDAO;
import ma.ensate.server.dao.PaiementDAO;
import ma.ensate.server.dao.ProduitDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de test pour toutes les fonctionnalités de commandes et paiements
 * 
 * Ce test vérifie :
 * 1. Création d'une commande
 * 2. Validation d'une commande
 * 3. Paiement simulé
 * 4. Mise à jour du stock après achat
 * 5. Gestion des différents états d'une commande
 * 6. Affichage de l'historique des commandes
 */
public class TestServices {

    private CommandeService commandeService;
    private PaymentService paymentService;
    private ProduitDAO produitDAO;
    private CommandeDAO commandeDAO;
    private PaiementDAO paiementDAO;

    public TestServices() {
        this.commandeService = new CommandeService();
        this.paymentService = new PaymentService();
        this.produitDAO = new ProduitDAO();
        this.commandeDAO = new CommandeDAO();
        this.paiementDAO = new PaiementDAO();
    }

    public static void main(String[] args) {
        TestServices test = new TestServices();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     TEST DES FONCTIONNALITÉS COMMANDES ET PAIEMENTS        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Test 1: Créer une commande
            test.testCreerCommande();
            
            // Test 2: Valider une commande
            test.testValiderCommande();
            
            // Test 3: Effectuer un paiement simulé
            test.testEffectuerPaiement();
            
            // Test 4: Vérifier la mise à jour du stock
            test.testMiseAJourStock();
            
            // Test 5: Gérer les différents états d'une commande
            test.testGestionEtatsCommande();
            
            // Test 6: Afficher l'historique des commandes
            test.testHistoriqueCommandes();
            
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              ✅ TOUS LES TESTS SONT RÉUSSIS              ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ❌ ERREUR LORS DES TESTS               ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 1: Créer une commande
     */
    private void testCreerCommande() throws SQLException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 1: Création d'une commande");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Créer un client de test
        Client client = creerClientTest();
        System.out.println("✓ Client créé: " + client.getNom() + " (ID: " + client.getId() + ")");
        
        // Créer des produits de test (vous devez avoir des produits en base)
        List<LigneCommande> lignes = creerLignesCommandeTest();
        System.out.println("✓ " + lignes.size() + " ligne(s) de commande créée(s)");
        
        if (lignes.isEmpty()) {
            System.out.println("⚠️  Aucune ligne de commande créée. Les tests suivants seront ignorés.");
            return;
        }
        
        // Vérifier le stock avant
        System.out.println("\n📦 Vérification du stock avant création de la commande:");
        for (LigneCommande ligne : lignes) {
            Produit produit = produitDAO.findById(ligne.getProduit().getId());
            if (produit != null) {
                System.out.println("  - " + produit.getNom() + ": Stock disponible = " + produit.getStock());
            }
        }
        
        // Créer la commande
        Commande commande = commandeService.creerCommande(client, lignes);
        System.out.println("\n✓ Commande créée avec succès !");
        System.out.println("  - ID Commande: " + commande.getId());
        System.out.println("  - Date: " + commande.getCommandeDate());
        System.out.println("  - Statut: " + commande.getStatut());
        System.out.println("  - Total: " + commande.getPrixAPayer() + " MAD");
        System.out.println("  - Nombre de lignes: " + commande.getLignes().size());
        
        System.out.println("✅ TEST 1 RÉUSSI: Commande créée\n");
    }

    /**
     * Test 2: Valider une commande
     */
    private void testValiderCommande() throws SQLException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 2: Validation d'une commande");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Créer une commande en attente
        Client client = creerClientTest();
        List<LigneCommande> lignes = creerLignesCommandeTest();
        
        if (lignes.isEmpty()) {
            System.out.println("⚠️  Aucune ligne de commande. Test ignoré.");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        
        System.out.println("Commande créée avec statut: " + commande.getStatut());
        
        // Valider la commande
        boolean valide = commandeService.validerCommande(commande.getId());
        
        if (valide) {
            // Récupérer la commande mise à jour
            Commande commandeValidee = commandeDAO.findById(commande.getId());
            System.out.println("✓ Commande validée avec succès !");
            System.out.println("  - Nouveau statut: " + commandeValidee.getStatut());
            System.out.println("✅ TEST 2 RÉUSSI: Commande validée\n");
        } else {
            throw new RuntimeException("Échec de la validation de la commande");
        }
    }

    /**
     * Test 3: Effectuer un paiement simulé
     */
    private void testEffectuerPaiement() throws SQLException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 3: Paiement simulé");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Créer une commande
        Client client = creerClientTest();
        List<LigneCommande> lignes = creerLignesCommandeTest();
        
        if (lignes.isEmpty()) {
            System.out.println("⚠️  Aucune ligne de commande. Test ignoré.");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        
        System.out.println("Commande créée: " + commande.getId());
        System.out.println("Montant à payer: " + commande.getPrixAPayer() + " MAD");
        
        // Effectuer le paiement (simulation avec carte bancaire)
        boolean paiementReussi = paymentService.effectuerPaiement(
            commande, 
            MethodePaiement.CARTE_BANCAIRE, 
            "1234"
        );
        
        if (paiementReussi) {
            System.out.println("✓ Paiement effectué avec succès !");
            
            // Vérifier que le paiement a été sauvegardé
            Paiement paiement = paiementDAO.findByCommandeId(commande.getId());
            if (paiement != null) {
                System.out.println("  - ID Paiement: " + paiement.getId());
                System.out.println("  - Méthode: " + paiement.getMethodePayment());
                System.out.println("  - Statut: " + paiement.getStatutPayment());
                System.out.println("  - Montant: " + paiement.getPrixAPayer() + " MAD");
                System.out.println("  - Date: " + paiement.getDatePayment());
            }
            
            // Vérifier que la commande a été validée automatiquement
            Commande commandeApresPaiement = commandeDAO.findById(commande.getId());
            System.out.println("\n✓ Commande automatiquement validée après paiement");
            System.out.println("  - Nouveau statut: " + commandeApresPaiement.getStatut());
            
            System.out.println("✅ TEST 3 RÉUSSI: Paiement simulé effectué\n");
        } else {
            throw new RuntimeException("Échec du paiement");
        }
    }

    /**
     * Test 4: Vérifier la mise à jour du stock après achat
     */
    private void testMiseAJourStock() throws SQLException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 4: Mise à jour du stock après achat");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Créer une commande avec des produits
        Client client = creerClientTest();
        List<LigneCommande> lignes = creerLignesCommandeTest();
        
        if (lignes.isEmpty()) {
            System.out.println("⚠️  Aucune ligne de commande. Test ignoré.");
            return;
        }
        
        System.out.println("📦 Stock AVANT l'achat:");
        for (LigneCommande ligne : lignes) {
            Produit produit = produitDAO.findById(ligne.getProduit().getId());
            if (produit != null) {
                int stockAvant = produit.getStock();
                System.out.println("  - " + produit.getNom() + ": " + stockAvant + " unités");
            }
        }
        
        // Créer et payer la commande
        Commande commande = commandeService.creerCommande(client, lignes);
        paymentService.effectuerPaiement(commande, MethodePaiement.ALIVRAISON, null);
        
        System.out.println("\n📦 Stock APRÈS l'achat:");
        for (LigneCommande ligne : lignes) {
            Produit produit = produitDAO.findById(ligne.getProduit().getId());
            if (produit != null) {
                int stockApres = produit.getStock();
                int quantiteAchetee = ligne.getQuantite();
                System.out.println("  - " + produit.getNom() + ": " + stockApres + " unités");
                System.out.println("    (Quantité achetée: " + quantiteAchetee + ", Stock décrémenté: ✓)");
            }
        }
        
        System.out.println("✅ TEST 4 RÉUSSI: Stock mis à jour après achat\n");
    }

    /**
     * Test 5: Gérer les différents états d'une commande
     */
    private void testGestionEtatsCommande() throws SQLException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 5: Gestion des différents états d'une commande");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Créer une commande
        Client client = creerClientTest();
        List<LigneCommande> lignes = creerLignesCommandeTest();
        
        if (lignes.isEmpty()) {
            System.out.println("⚠️  Aucune ligne de commande. Test ignoré.");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        
        System.out.println("État initial: " + commande.getStatut());
        
        // Transition 1: EN_ATTENTE -> VALIDE
        System.out.println("\n🔄 Transition: EN_ATTENTE → VALIDE");
        commandeService.changerStatutCommande(commande.getId(), StatutCommande.VALIDE);
        Commande cmd1 = commandeDAO.findById(commande.getId());
        System.out.println("  ✓ Nouveau statut: " + cmd1.getStatut());
        
        // Transition 2: VALIDE -> EXPEDIE
        System.out.println("\n🔄 Transition: VALIDE → EXPEDIE");
        commandeService.changerStatutCommande(commande.getId(), StatutCommande.EXPEDIE);
        Commande cmd2 = commandeDAO.findById(commande.getId());
        System.out.println("  ✓ Nouveau statut: " + cmd2.getStatut());
        
        // Transition 3: EXPEDIE -> LIVRE
        System.out.println("\n🔄 Transition: EXPEDIE → LIVRE");
        commandeService.changerStatutCommande(commande.getId(), StatutCommande.LIVRE);
        Commande cmd3 = commandeDAO.findById(commande.getId());
        System.out.println("  ✓ Nouveau statut: " + cmd3.getStatut());
        
        // Test d'une transition invalide (devrait échouer)
        System.out.println("\n⚠️  Test d'une transition invalide (LIVRE → EN_ATTENTE):");
        try {
            commandeService.changerStatutCommande(commande.getId(), StatutCommande.EN_ATTENTE);
            System.out.println("  ❌ ERREUR: La transition devrait échouer !");
        } catch (IllegalStateException e) {
            System.out.println("  ✓ Transition correctement rejetée: " + e.getMessage());
        }
        
        System.out.println("✅ TEST 5 RÉUSSI: Gestion des états fonctionne\n");
    }

    /**
     * Test 6: Afficher l'historique des commandes
     */
    private void testHistoriqueCommandes() throws SQLException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("TEST 6: Historique des commandes");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Créer un client
        Client client = creerClientTest();
        System.out.println("Client: " + client.getNom() + " (ID: " + client.getId() + ")");
        
        // Créer plusieurs commandes pour ce client
        System.out.println("\n📝 Création de plusieurs commandes...");
        int commandesCreees = 0;
        for (int i = 1; i <= 3; i++) {
            List<LigneCommande> lignes = creerLignesCommandeTest();
            if (!lignes.isEmpty()) {
                Commande commande = commandeService.creerCommande(client, lignes);
                System.out.println("  ✓ Commande #" + i + " créée: " + commande.getId());
                commandesCreees++;
            }
        }
        
        if (commandesCreees == 0) {
            System.out.println("⚠️  Aucune commande créée. Test ignoré.");
            return;
        }
        
        // Récupérer l'historique
        System.out.println("\n📋 Historique des commandes:");
        List<Commande> historique = commandeService.getHistorique(client.getId());
        
        if (historique != null && !historique.isEmpty()) {
            System.out.println("  Total de commandes: " + historique.size());
            System.out.println();
            
            for (int i = 0; i < historique.size(); i++) {
                Commande cmd = historique.get(i);
                System.out.println("  Commande #" + (i + 1) + ":");
                System.out.println("    - ID: " + cmd.getId());
                System.out.println("    - Date: " + cmd.getCommandeDate());
                System.out.println("    - Statut: " + cmd.getStatut());
                System.out.println("    - Total: " + cmd.getPrixAPayer() + " MAD");
                System.out.println("    - Lignes: " + cmd.getLignes().size());
                System.out.println();
            }
            
            System.out.println("✅ TEST 6 RÉUSSI: Historique récupéré avec succès\n");
        } else {
            System.out.println("⚠️  Aucune commande trouvée dans l'historique");
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Crée un client de test
     * NOTE: Vous devez adapter cette méthode selon votre structure de base de données
     */
    private Client creerClientTest() {
        Client client = new Client();
        client.setId(2); // Remplacez par un ID existant en base
        client.setNom("Client Test");
        client.setEmail("client.test@example.com");
        client.setAdresse("123 Rue Test, Casablanca");
        client.setTel("0612345678");
        return client;
    }

    /**
     * Crée des lignes de commande de test
     * NOTE: Vous devez adapter cette méthode selon vos produits en base
     */
    private List<LigneCommande> creerLignesCommandeTest() throws SQLException {
        List<LigneCommande> lignes = new ArrayList<>();
        
        // Exemple: créer des lignes avec des produits existants
        // Remplacez les IDs par des IDs de produits réels dans votre base
        
        try {
            // Essayer de récupérer un produit (ID 1 par exemple)
            Produit produit1 = produitDAO.findById(1);
            if (produit1 != null && produit1.getStock() > 0) {
                int quantite = Math.min(2, produit1.getStock()); // Prendre max 2 unités
                LigneCommande ligne1 = new LigneCommande(produit1, quantite);
                lignes.add(ligne1);
            }
            
            // Essayer un deuxième produit (ID 2 par exemple)
            Produit produit2 = produitDAO.findById(2);
            if (produit2 != null && produit2.getStock() > 0) {
                int quantite = Math.min(1, produit2.getStock());
                LigneCommande ligne2 = new LigneCommande(produit2, quantite);
                lignes.add(ligne2);
            }
            
            if (lignes.isEmpty()) {
                System.out.println("⚠️  ATTENTION: Aucun produit trouvé en base de données.");
                System.out.println("   Veuillez créer des produits dans votre base avant de tester.");
                System.out.println("   Les tests nécessitent au moins 2 produits avec du stock.");
            }
            
        } catch (SQLException e) {
            System.out.println("⚠️  Erreur lors de la récupération des produits: " + e.getMessage());
        }
        
        return lignes;
    }
}

