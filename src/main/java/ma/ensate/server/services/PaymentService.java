package ma.ensate.server.services;

import ma.ensate.models.*;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.PaiementRequest;
import ma.ensate.server.dao.*;
import ma.ensate.server.network.ClientIPRegistry;
import ma.ensate.server.network.UDPNotificationServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

public class PaymentService {

    private static final Logger logger = LogManager.getLogger(PaymentService.class);

    private final PaiementDAO paiementDAO = new PaiementDAO();
    private final CommandeDAO commandeDAO = new CommandeDAO();
    private final ProduitDAO produitDAO = new ProduitDAO();
    private final ServicePanier servicePanier = new ServicePanier();

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

    public Response traiterPaiement(PaiementRequest req) {
        try {
            if (req == null || req.getCommandeId() == null || req.getMethodePaiement() == null) {
                return new Response(false, "Requete de paiement invalide");
            }

            MethodePaiement methode;
            try {
                methode = MethodePaiement.valueOf(req.getMethodePaiement());
            } catch (IllegalArgumentException e) {
                return new Response(false, "Methode de paiement invalide : " + req.getMethodePaiement());
            }

            if (methode == MethodePaiement.CARTE_BANCAIRE) {
                if (req.getCardLast4() == null || req.getCardLast4().length() != 4) {
                    return new Response(false, "Les 4 derniers chiffres de la carte sont requis");
                }
            }

            Commande commande = commandeDAO.findById(req.getCommandeId());
            if (commande == null) {
                return new Response(false, "Commande introuvable");
            }

            if (commande.getClient() != null
                    && PaymentRateLimiter.isReplayAttack(
                    String.valueOf(commande.getClient().getId()),
                    commande.getPrixAPayer())) {
                logger.warn("Replay attack evite pour la commande: {}", req.getCommandeId());
                return new Response(
                        false,
                        "Paiement en cours ou deja effectue recemment (anti-rejeu). Veuillez patienter.");
            }

            boolean success = effectuerPaiement(commande, methode, req.getCardLast4());
            if (!success) {
                return new Response(false, "Echec du paiement");
            }

            if (commande.getClient() != null) {
                servicePanier.viderPanier(commande.getClient().getId());

                String destinataireIP = ClientIPRegistry.getIP(commande.getClient().getId());
                if (destinataireIP != null) {
                    int port = ClientIPRegistry.getPort(commande.getClient().getId());
                    UDPNotificationServer.notifierCommandeValidee(
                            destinataireIP,
                            port,
                            req.getCommandeId());
                } else {
                    logger.warn("Client non connecte, notification ignoree");
                }
            }

            Paiement paiement = getPaiementByCommandeId(req.getCommandeId());
            logger.info("Paiement effectue : {}", req.getCommandeId());
            return new Response(true, "Paiement effectue avec succes", paiement);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response(false, e.getMessage());
        } catch (SQLException e) {
            return new Response(false, "Erreur base de donnees : " + e.getMessage());
        }
    }

    public Paiement getPaiementByCommandeId(String id) throws SQLException {
        if (id == null) return null;
        return paiementDAO.findByCommandeId(id);
    }
}
