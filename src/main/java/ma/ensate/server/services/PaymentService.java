package ma.ensate.server.services;

import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.MethodePaiement;
import ma.ensate.models.Paiement;
import ma.ensate.models.StatutCommande;
import ma.ensate.models.StatutPaiement;
import ma.ensate.server.dao.CommandeDAO;
import ma.ensate.server.dao.PaiementDAO;
import ma.ensate.server.dao.ProduitDAO;

import java.sql.SQLException;

public class PaymentService {

    private PaiementDAO paiementDAO;
    private CommandeDAO commandeDAO;
    private ProduitDAO produitDAO;

    public PaymentService() {
        this.paiementDAO = new PaiementDAO();
        this.commandeDAO = new CommandeDAO();
        this.produitDAO = new ProduitDAO();
    }

    /**
     * Effectue un paiement simulé pour une commande
     * 1. Crée le paiement
     * 2. Sauvegarde le paiement en base
     * 3. Valide la commande (change le statut à VALIDE)
     * 4. Met à jour le stock des produits (décrémente les quantités achetées)
     * 
     * @param commande La commande à payer
     * @param methode La méthode de paiement (ALIVRAISON ou CARTE_BANCAIRE)
     * @param cardLast4 Les 4 derniers chiffres de la carte (optionnel, null si ALIVRAISON)
     * @return true si le paiement a réussi, false sinon
     */
    public boolean effectuerPaiement(Commande commande, MethodePaiement methode, String cardLast4) 
            throws SQLException, IllegalStateException {
        
        // Validation de la commande
        if (commande == null) {
            throw new IllegalArgumentException("La commande ne peut pas être null");
        }
        if (commande.getId() == null || commande.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la commande ne peut pas être vide");
        }
        
        // Validation de la méthode de paiement
        if (methode == null) {
            throw new IllegalArgumentException("La méthode de paiement ne peut pas être null");
        }
        
        // Validation pour carte bancaire
        if (methode == MethodePaiement.CARTE_BANCAIRE) {
            if (cardLast4 == null || cardLast4.trim().isEmpty()) {
                throw new IllegalArgumentException("Les 4 derniers chiffres de la carte sont requis pour le paiement par carte");
            }
            if (!cardLast4.matches("\\d{4}")) {
                throw new IllegalArgumentException("Les 4 derniers chiffres de la carte doivent être 4 chiffres");
            }
        }
        
        // Vérifier que la commande existe et est en attente
        Commande commandeComplete = commandeDAO.findById(commande.getId());
        if (commandeComplete == null) {
            throw new IllegalArgumentException("Commande introuvable: " + commande.getId());
        }

        if (commandeComplete.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new IllegalStateException(
                "Impossible de payer une commande avec le statut: " + commandeComplete.getStatut()
            );
        }
        
        // Vérifier que la commande a un montant valide
        if (commandeComplete.getPrixAPayer() <= 0) {
            throw new IllegalStateException("Le montant de la commande doit être supérieur à 0");
        }
        
        // Vérifier que la commande a des lignes
        if (commandeComplete.getLignes() == null || commandeComplete.getLignes().isEmpty()) {
            throw new IllegalStateException("Impossible de payer une commande sans lignes");
        }

        // Vérifier que tous les produits ont encore suffisamment de stock
        for (LigneCommande ligne : commandeComplete.getLignes()) {
            if (ligne.getProduit() == null) {
                throw new IllegalStateException("Une ligne de commande contient un produit null");
            }
            if (!produitDAO.verifierStock(ligne.getProduit().getId(), ligne.getQuantite())) {
                throw new IllegalStateException(
                    "Stock insuffisant pour le produit: " + ligne.getProduit().getNom()
                );
            }
        }

        // Créer le paiement
        Paiement paiement = new Paiement();
        paiement.setCommandeId(commandeComplete.getId());
        paiement.setMethodePayment(methode);
        paiement.setStatutPayment(StatutPaiement.ACCEPTE);
        paiement.setPrixAPayer(commandeComplete.getPrixAPayer());
        paiement.setCardLast4(cardLast4);

        // Sauvegarder le paiement en base de données
        if (!paiementDAO.sauvegarder(paiement)) {
            throw new SQLException("Erreur lors de la sauvegarde du paiement");
        }

        // Valider la commande (changer le statut à VALIDE)
        if (!commandeDAO.mettreAJourStatut(commandeComplete.getId(), StatutCommande.VALIDE)) {
            throw new SQLException("Erreur lors de la validation de la commande");
        }

        // Mettre à jour le stock des produits (décrémenter les quantités achetées)
        for (LigneCommande ligne : commandeComplete.getLignes()) {
            if (!produitDAO.mettreAJourStock(ligne.getProduit().getId(), ligne.getQuantite())) {
                // Si la mise à jour du stock échoue, on pourrait faire un rollback
                // Pour l'instant, on lance une exception
                throw new SQLException(
                    "Erreur lors de la mise à jour du stock pour le produit: " + ligne.getProduit().getNom()
                );
            }
        }

        return true;
    }

    /**
     * Récupère le paiement associé à une commande
     */
    public Paiement getPaiementByCommandeId(String commandeId) throws SQLException {
        if (commandeId == null || commandeId.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la commande ne peut pas être vide");
        }
        return paiementDAO.findByCommandeId(commandeId);
    }
}
