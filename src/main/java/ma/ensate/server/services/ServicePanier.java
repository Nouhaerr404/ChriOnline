package ma.ensate.server.services;

import ma.ensate.models.LignePanier;
import ma.ensate.models.Panier;
import ma.ensate.models.Produit;
import ma.ensate.protocol.Response;
import ma.ensate.server.dao.PanierDAO;
import ma.ensate.server.dao.ProduitDAO;

import java.util.List;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServicePanier — logique métier du panier.
 *
 * Orchestration :
 *  - valide les règles métier (stock, quantité, appartenance)
 *  - délègue les opérations SQL à PanierDAO
 *  - utilise ProduitDAO pour vérifier le stock (Personne 2)
 *
 * Toutes les méthodes publiques retournent un String décrivant le résultat ;
 * ce String sera encapsulé dans un Response.java par ClientHandler (Personne 1).
 */
public class ServicePanier {

    private static final Logger LOGGER = Logger.getLogger(ServicePanier.class.getName());

    private final PanierDAO  panierDAO;
    private final ProduitDAO produitDAO;

    public ServicePanier() {
        this.panierDAO  = new PanierDAO();
        this.produitDAO = new ProduitDAO();
    }

    // Constructeur par injection (utile pour les tests)
    public ServicePanier(PanierDAO panierDAO, ProduitDAO produitDAO) {
        this.panierDAO  = panierDAO;
        this.produitDAO = produitDAO;
    }

    // =========================================================================
    // AFFICHER LE PANIER
    // =========================================================================

    /**
     * Charge le panier complet du client (lignes incluses).
     *
     * @param clientId  id du client connecté
     * @return Panier chargé, ou null si erreur
     */
    public Panier obtenirPanier(int clientId) {
        Panier panier = panierDAO.obtenirOuCreerPanier(clientId);
        if (panier == null) return null;

        List<LignePanier> lignes = panierDAO.obtenirLignesDuPanier(panier.getId());
        panier.setLignes(lignes);
        return panier;
    }

    // =========================================================================
    // AJOUTER UN PRODUIT
    // =========================================================================

    /**
     * Ajoute un produit au panier après validation des règles métier.
     */
    public String ajouterProduit(int clientId, int produitId, int quantite) {
        if (quantite <= 0) return "ERREUR: La quantité doit être supérieure à 0.";

        Produit produit;
        try {
            produit = produitDAO.findById(produitId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur findById", e);
            return "ERREUR: Erreur d'accès aux produits.";
        }
        if (produit == null) return "ERREUR: Produit introuvable (id=" + produitId + ").";
        if (produit.getStock() <= 0) return "ERREUR: Le produit '" + produit.getNom() + "' est en rupture de stock.";

        if (quantite > produit.getStock()) {
            return "ERREUR: Stock insuffisant. Disponible : " + produit.getStock() + ", demandé : " + quantite + ".";
        }

        Panier panier = panierDAO.obtenirOuCreerPanier(clientId);
        if (panier == null) return "ERREUR: Impossible d'accéder au panier.";

        List<LignePanier> lignes = panierDAO.obtenirLignesDuPanier(panier.getId());
        int qteDejaAuPanier = lignes.stream()
                .filter(l -> l.getProduitId() == produitId)
                .mapToInt(LignePanier::getQuantite)
                .sum();

        if (qteDejaAuPanier + quantite > produit.getStock()) {
            return "ERREUR: Quantité totale (" + (qteDejaAuPanier + quantite) + ") dépasse le stock disponible (" + produit.getStock() + ").";
        }

        boolean ok = panierDAO.ajouterOuMettreAJourLigne(panier.getId(), produitId, quantite, produit.getPrix());
        return ok ? "OK: '" + produit.getNom() + "' (x" + quantite + ") ajouté au panier." : "ERREUR: Erreur d'ajout.";
    }

    // =========================================================================
    // MODIFIER LA QUANTITÉ
    // =========================================================================

    public String modifierQuantite(int clientId, int produitId, int quantite) {
        if (quantite < 0) return "ERREUR: La quantité ne peut pas être négative.";

        Panier panier = panierDAO.obtenirOuCreerPanier(clientId);
        if (panier == null) return "ERREUR: Impossible d'accéder au panier.";

        if (quantite == 0) return supprimerProduit(clientId, produitId);

        Produit produit;
        try {
            produit = produitDAO.findById(produitId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur findById", e);
            return "ERREUR: Erreur d'accès aux produits.";
        }
        if (produit == null) return "ERREUR: Produit introuvable.";
        if (quantite > produit.getStock()) return "ERREUR: Stock insuffisant. Disponible : " + produit.getStock() + ".";

        boolean ok = panierDAO.modifierQuantite(panier.getId(), produitId, quantite, produit.getPrix());
        return ok ? "OK: Quantité mise à jour (" + quantite + " x '" + produit.getNom() + "')." : "ERREUR: Impossible de modifier.";
    }

    // =========================================================================
    // SUPPRIMER UN PRODUIT
    // =========================================================================

    public String supprimerProduit(int clientId, int produitId) {
        Panier panier = panierDAO.obtenirOuCreerPanier(clientId);
        if (panier == null) return "ERREUR: Impossible d'accéder au panier.";

        boolean ok = panierDAO.supprimerLigne(panier.getId(), produitId);
        return ok ? "OK: Produit retiré du panier." : "ERREUR: Produit non trouvé.";
    }

    // =========================================================================
    // VIDER LE PANIER
    // =========================================================================

    public boolean viderPanier(int clientId) {
        Panier panier = panierDAO.obtenirOuCreerPanier(clientId);
        if (panier == null) return false;
        return panierDAO.viderPanier(panier.getId());
    }

    // =========================================================================
    // RESPONSE WRAPPERS (For ClientHandler)
    // =========================================================================

    public Response obtenirPanierResponse(int clientId) {
        Panier panier = obtenirPanier(clientId);
        if (panier == null) {
            return new Response(false, "ERREUR: Impossible de charger le panier.");
        }

        StringBuilder sb = new StringBuilder();
        for (LignePanier lp : panier.getLignes()) {
            sb.append(lp.getProduitId()).append("|")
              .append(lp.getProduitNom()).append("|")
              .append(lp.getPrixUnitaire()).append("|")
              .append(lp.getQuantite()).append("|")
              .append(lp.getSubtotal()).append("\n");
        }
        sb.append("TOTAL:").append(String.format("%.2f", panier.getTotal()));

        return new Response(true, "Panier chargé.", sb.toString());
    }

    public Response ajouterProduitResponse(int clientId, int produitId, int quantite) {
        String msg = ajouterProduit(clientId, produitId, quantite);
        return new Response(msg.startsWith("OK"), msg);
    }

    public Response modifierQuantiteResponse(int clientId, int produitId, int quantite) {
        String msg = modifierQuantite(clientId, produitId, quantite);
        return new Response(msg.startsWith("OK"), msg);
    }

    public Response supprimerProduitResponse(int clientId, int produitId) {
        String msg = supprimerProduit(clientId, produitId);
        return new Response(msg.startsWith("OK"), msg);
    }

    public Response viderPanierResponse(int clientId) {
        boolean ok = viderPanier(clientId);
        return ok ? new Response(true, "OK: Panier vidé.") : new Response(false, "ERREUR: Impossible de vider le panier.");
    }
}
