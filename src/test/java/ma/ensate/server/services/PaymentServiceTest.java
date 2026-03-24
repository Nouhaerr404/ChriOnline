package ma.ensate.server.services;

import ma.ensate.models.Client;
import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.MethodePaiement;
import ma.ensate.models.Paiement;
import ma.ensate.models.Produit;
import ma.ensate.models.StatutCommande;
import ma.ensate.server.dao.ClientDAO;
import ma.ensate.server.dao.ProduitDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests unitaires pour PaymentService
 */
public class PaymentServiceTest {
    
    private PaymentService paymentService;
    private CommandeService commandeService;
    private ClientDAO clientDAO;
    private ProduitDAO produitDAO;
    
    public PaymentServiceTest() {
        this.paymentService = new PaymentService();
        this.commandeService = new CommandeService();
        this.clientDAO = new ClientDAO();
        this.produitDAO = new ProduitDAO();
    }
    
    /**
     * Test de paiement avec carte bancaire
     */
    public void testEffectuerPaiementCarte() throws Exception {
        System.out.println("Test: Paiement avec carte bancaire");
        
        // Créer une commande
        Client client = clientDAO.findById(2);
        if (client == null) {
            System.out.println("  ⚠️  Client ID 2 non trouvé, test ignoré");
            return;
        }
        
        List<LigneCommande> lignes = new ArrayList<>();
        Produit produit = produitDAO.findById(2);
        if (produit != null && produit.getStock() > 0) {
            lignes.add(new LigneCommande(produit, 1));
        }
        
        if (lignes.isEmpty()) {
            System.out.println("  ⚠️  Aucun produit disponible, test ignoré");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        
        // Effectuer le paiement
        boolean success = paymentService.effectuerPaiement(commande, MethodePaiement.CARTE_BANCAIRE, "1234");
        
        // Vérifier
        assert success : "Le paiement devrait réussir";
        Commande commandeApresPaiement = commandeService.getCommandeById(commande.getId());
        assert commandeApresPaiement.getStatut() == StatutCommande.VALIDE : "La commande devrait être validée";
        
        Paiement paiement = paymentService.getPaiementByCommandeId(commande.getId());
        assert paiement != null : "Le paiement devrait être créé";
        assert paiement.getMethodePayment() == MethodePaiement.CARTE_BANCAIRE : "La méthode devrait être CARTE_BANCAIRE";
        assert "1234".equals(paiement.getCardLast4()) : "Les 4 derniers chiffres devraient être 1234";
        
        System.out.println("  ✅ Test réussi");
    }
    
    /**
     * Test de paiement à la livraison
     */
    public void testEffectuerPaiementLivraison() throws Exception {
        System.out.println("Test: Paiement à la livraison");
        
        Client client = clientDAO.findById(2);
        if (client == null) {
            System.out.println("    Client ID 2 non trouvé, test ignoré");
            return;
        }
        
        List<LigneCommande> lignes = new ArrayList<>();
        Produit produit = produitDAO.findById(2);
        if (produit != null && produit.getStock() > 0) {
            lignes.add(new LigneCommande(produit, 1));
        }
        
        if (lignes.isEmpty()) {
            System.out.println("    Aucun produit disponible, test ignoré");
            return;
        }
        
        Commande commande = commandeService.creerCommande(client, lignes);
        
        // Effectuer le paiement
        boolean success = paymentService.effectuerPaiement(commande, MethodePaiement.ALIVRAISON, null);
        
        // Vérifier
        assert success : "Le paiement devrait réussir";
        Paiement paiement = paymentService.getPaiementByCommandeId(commande.getId());
        assert paiement.getMethodePayment() == MethodePaiement.ALIVRAISON : "La méthode devrait être ALIVRAISON";
        
        System.out.println("  ✅ Test réussi");
    }
    
    /**
     * Test de paiement avec commande null
     */
    public void testPaiementCommandeNull() {
        System.out.println("Test: Paiement avec commande null");
        
        try {
            paymentService.effectuerPaiement(null, MethodePaiement.ALIVRAISON, null);
            System.out.println("  ❌ Test échoué: devrait lancer une exception");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Test réussi: exception attendue - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ❌ Test échoué: exception inattendue - " + e.getMessage());
        }
    }
    
    /**
     * Test de paiement avec carte sans numéro
     */
    public void testPaiementCarteSansNumero() {
        System.out.println("Test: Paiement par carte sans numéro");
        
        try {
            Commande commande = new Commande();
            commande.setId("test-id");
            paymentService.effectuerPaiement(commande, MethodePaiement.CARTE_BANCAIRE, null);
            System.out.println("  ❌ Test échoué: devrait lancer une exception");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Test réussi: exception attendue - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ❌ Test réussi: exception attendue - " + e.getMessage());
        }
    }
    
    /**
     * Test de récupération d'un paiement
     */
    public void testGetPaiement() throws Exception {
        System.out.println("Test: Récupération d'un paiement");
        
        Client client = clientDAO.findById(2);
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
        paymentService.effectuerPaiement(commande, MethodePaiement.ALIVRAISON, null);
        
        Paiement paiement = paymentService.getPaiementByCommandeId(commande.getId());
        
        assert paiement != null : "Le paiement devrait être trouvé";
        assert paiement.getCommandeId().equals(commande.getId()) : "L'ID de commande devrait correspondre";
        
        System.out.println("  ✅ Test réussi");
    }
    
    /**
     * Exécute tous les tests
     */
    public static void main(String[] args) {
        PaymentServiceTest test = new PaymentServiceTest();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         TESTS UNITAIRES - PAYMENTSERVICE                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            test.testEffectuerPaiementCarte();
            test.testEffectuerPaiementLivraison();
            test.testPaiementCommandeNull();
            test.testPaiementCarteSansNumero();
            test.testGetPaiement();
            
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

