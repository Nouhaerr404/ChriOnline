package ma.ensate.server.services;

import ma.ensate.models.*;
import ma.ensate.server.dao.*;
import java.sql.SQLException;

public class PaymentService {

    private PaiementDAO paiementDAO = new PaiementDAO();
    private CommandeDAO commandeDAO = new CommandeDAO();
    private ProduitDAO produitDAO = new ProduitDAO();

    /**
     * Gère le paiement d'une commande
     */
    public boolean effectuerPaiement(Commande cmd, MethodePaiement methode, String last4) throws SQLException {
        
        // 1. Vérifications de base
        if (cmd == null || cmd.getId() == null) return false;

        Commande fullCmd = commandeDAO.findById(cmd.getId());
        if (fullCmd == null || fullCmd.getStatut() != StatutCommande.EN_ATTENTE) {
            return false;
        }

        // 2. Création de l'enregistrement de paiement
        Paiement paiem = new Paiement();
        paiem.setCommandeId(fullCmd.getId());
        paiem.setMethodePayment(methode);
        paiem.setStatutPayment(StatutPaiement.ACCEPTE);
        paiem.setPrixAPayer(fullCmd.getPrixAPayer());
        paiem.setCardLast4(last4);

        // 3. Mise à jour des bases de données
        boolean ok = paiementDAO.sauvegarder(paiem);
        
        if (ok) {
            // Passer la commande en statut VALIDÉ
            commandeDAO.mettreAJourStatut(fullCmd.getId(), StatutCommande.VALIDE);
            
            // Diminuer le stock pour chaque produit
            for (LigneCommande ligne : fullCmd.getLignes()) {
                if (ligne.getProduit() != null) {
                    produitDAO.mettreAJourStock(ligne.getProduit().getId(), ligne.getQuantite());
                }
            }
            return true;
        }

        return false;
    }

    public Paiement getPaiementByCommandeId(String id) throws SQLException {
        if (id == null) return null;
        return paiementDAO.findByCommandeId(id);
    }
}
