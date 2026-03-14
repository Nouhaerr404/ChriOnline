package ma.ensate.server.services;

import ma.ensate.models.Client;
import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.Produit;
import ma.ensate.models.StatutCommande;
import ma.ensate.server.dao.ClientDAO;
import ma.ensate.server.dao.ProduitDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests unitaires pour CommandeService
 * 
 * Note: Ces tests nécessitent une base de données configurée avec des données de test
 */
public class CommandeServiceTest {
    
    private CommandeService commandeService;
    private ClientDAO clientDAO;
    private ProduitDAO produitDAO;
    
    public CommandeServiceTest() {
        this.commandeService = new CommandeService();
        this.clientDAO = new ClientDAO();
        this.produitDAO = new ProduitDAO();
    }
    
    /**
     * Test de création d'une commande valide
     */
    public void testCreerCommandeValide() throws Exception {
        System.out.println("Test: Création d'une commande valide");
        
        // Préparer les données
        Client client = clientDAO.findById(1);
        if (client == null) {
            System.out.println("  ⚠️  Client ID 1 non trouvé, test ignoré");
            return;
        }
        
        List<LigneCommande> lignes = new ArrayList<>();
        Produit produit = produitDAO.findById(1);
        if (produit != null && produit.getStock() > 0) {
            lignes.add(new LigneCommande(produit, 1));
        }
        
        if (lignes.isEmpty()) {
            System.out.println("  ⚠️  Aucun produit disponible, test ignoré");
            return;
        }
        
        // Exécuter
        Commande commande = commandeService.creerCommande(client, lignes);
        
        // Vérifier
        assert commande != null : "La commande ne devrait pas être null";
        assert commande.getId() != null : "L'ID de la commande ne devrait pas être null";
        assert commande.getStatut() == StatutCommande.EN_ATTENTE : "Le statut devrait être EN_ATTENTE";
        assert commande.getPrixAPayer() > 0 : "Le prix devrait être supérieur à 0";
        assert !commande.getLignes().isEmpty() : "La commande devrait avoir des lignes";
        
        System.out.println("  ✅ Test réussi");
    }
    
    /**
     * Test de création d'une commande avec client null
     */
    public void testCreerCommandeClientNull() {
        System.out.println("Test: Création d'une commande avec client null");
        
        try {
            List<LigneCommande> lignes = new ArrayList<>();
            commandeService.creerCommande(null, lignes);
            System.out.println("  ❌ Test échoué: devrait lancer une exception");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Test réussi: exception attendue - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ❌ Test échoué: exception inattendue - " + e.getMessage());
        }
    }
    
    /**
     * Test de création d'une commande avec lignes vides
     */
    public void testCreerCommandeLignesVides() {
        System.out.println("Test: Création d'une commande avec lignes vides");
        
        try {
            Client client = new Client();
            client.setId(1);
            commandeService.creerCommande(client, new ArrayList<>());
            System.out.println("  ❌ Test échoué: devrait lancer une exception");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Test réussi: exception attendue - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ❌ Test échoué: exception inattendue - " + e.getMessage());
        }
    }
    
    /**
     * Test de validation d'une commande
     */
    public void testValiderCommande() throws Exception {
        System.out.println("Test: Validation d'une commande");
        
        // Créer une commande
        Client client = clientDAO.findById(1);
        if (client == null) {
            System.out.println("  ⚠️  Client ID 1 non trouvé, test ignoré");
            return;
        }
        
        List<LigneCommande> lignes = new ArrayList<>();
        Produit produit = produitDAO.findById(1);
        if (produit != null && produit.getStock() > 0) {
            lignes.add(new LigneCommande(produit, 1));
        }
        
        if (lignes.isEmpty()) {
            System.out.println("  ⚠️  Aucun produit disponible, test ignoré");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        
        // Valider
        boolean success = commandeService.validerCommande(commande.getId());
        
        // Vérifier
        assert success : "La validation devrait réussir";
        Commande commandeValidee = commandeService.getCommandeById(commande.getId());
        assert commandeValidee.getStatut() == StatutCommande.VALIDE : "Le statut devrait être VALIDE";
        
        System.out.println("  ✅ Test réussi");
    }
    
    /**
     * Test de validation avec ID invalide
     */
    public void testValiderCommandeIdInvalide() {
        System.out.println("Test: Validation avec ID invalide");
        
        try {
            commandeService.validerCommande("");
            System.out.println("  ❌ Test échoué: devrait lancer une exception");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Test réussi: exception attendue - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ❌ Test échoué: exception inattendue - " + e.getMessage());
        }
    }
    
    /**
     * Test de changement de statut
     */
    public void testChangerStatutCommande() throws Exception {
        System.out.println("Test: Changement de statut d'une commande");
        
        // Créer et valider une commande
        Client client = clientDAO.findById(1);
        if (client == null) {
            System.out.println("  ⚠️  Client ID 1 non trouvé, test ignoré");
            return;
        }
        
        List<LigneCommande> lignes = new ArrayList<>();
        Produit produit = produitDAO.findById(1);
        if (produit != null && produit.getStock() > 0) {
            lignes.add(new LigneCommande(produit, 1));
        }
        
        if (lignes.isEmpty()) {
            System.out.println("  ⚠️  Aucun produit disponible, test ignoré");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        commandeService.validerCommande(commande.getId());
        
        // Changer le statut
        boolean success = commandeService.changerStatutCommande(commande.getId(), StatutCommande.EXPEDIE);
        
        // Vérifier
        assert success : "Le changement de statut devrait réussir";
        Commande commandeModifiee = commandeService.getCommandeById(commande.getId());
        assert commandeModifiee.getStatut() == StatutCommande.EXPEDIE : "Le statut devrait être EXPEDIE";
        
        System.out.println("  ✅ Test réussi");
    }
    
    /**
     * Test de transition invalide
     */
    public void testTransitionInvalide() throws Exception {
        System.out.println("Test: Transition de statut invalide");
        
        // Créer une commande et la faire passer à LIVRE
        Client client = clientDAO.findById(1);
        if (client == null) {
            System.out.println("  ⚠️  Client ID 1 non trouvé, test ignoré");
            return;
        }
        
        List<LigneCommande> lignes = new ArrayList<>();
        Produit produit = produitDAO.findById(1);
        if (produit != null && produit.getStock() > 0) {
            lignes.add(new LigneCommande(produit, 1));
        }
        
        if (lignes.isEmpty()) {
            System.out.println("  ⚠️  Aucun produit disponible, test ignoré");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        commandeService.validerCommande(commande.getId());
        commandeService.changerStatutCommande(commande.getId(), StatutCommande.EXPEDIE);
        commandeService.changerStatutCommande(commande.getId(), StatutCommande.LIVRE);
        
        // Essayer une transition invalide
        try {
            commandeService.changerStatutCommande(commande.getId(), StatutCommande.EN_ATTENTE);
            System.out.println("  ❌ Test échoué: devrait lancer une exception");
        } catch (IllegalStateException e) {
            System.out.println("  ✅ Test réussi: exception attendue - " + e.getMessage());
        }
    }
    
    /**
     * Test de récupération de l'historique
     */
    public void testGetHistorique() throws Exception {
        System.out.println("Test: Récupération de l'historique");
        
        int clientId = 1;
        List<Commande> historique = commandeService.getHistorique(clientId);
        
        assert historique != null : "L'historique ne devrait pas être null";
        
        System.out.println("  ✅ Test réussi - " + historique.size() + " commande(s) trouvée(s)");
    }
    
    /**
     * Exécute tous les tests
     */
    public static void main(String[] args) {
        CommandeServiceTest test = new CommandeServiceTest();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         TESTS UNITAIRES - COMMANDESERVICE                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            test.testCreerCommandeValide();
            test.testCreerCommandeClientNull();
            test.testCreerCommandeLignesVides();
            test.testValiderCommande();
            test.testValiderCommandeIdInvalide();
            test.testChangerStatutCommande();
            test.testTransitionInvalide();
            test.testGetHistorique();
            
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              ✅ TOUS LES TESTS SONT TERMINÉS               ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ❌ ERREUR LORS DES TESTS               ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            e.printStackTrace();
        }
    }
}

