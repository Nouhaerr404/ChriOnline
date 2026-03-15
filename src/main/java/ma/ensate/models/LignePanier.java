package ma.ensate.models;

import java.io.Serializable;


/**
 * Modèle LignePanier — correspond à la table `ligne_panier`.
 * Représente un produit et sa quantité dans un panier.
 */
public class LignePanier {

    private int    id;
    private int    panierId;
    private int    produitId;
    private String produitNom;     // cache du nom pour l'affichage
    private double prixUnitaire;   // cache du prix unitaire
    private int    quantite;
    private double subtotal;       // prixUnitaire × quantite

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public LignePanier() {}

    /**
     * Constructeur principal utilisé lors de l'ajout au panier.
     */
    public LignePanier(int panierId, int produitId, String produitNom,
                       double prixUnitaire, int quantite) {
        this.panierId      = panierId;
        this.produitId     = produitId;
        this.produitNom    = produitNom;
        this.prixUnitaire  = prixUnitaire;
        this.quantite      = quantite;
        this.subtotal      = prixUnitaire * quantite;
    }

    /**
     * Constructeur complet (utilisé lors du chargement depuis la BDD).
     */
    public LignePanier(int id, int panierId, int produitId, String produitNom,
                       double prixUnitaire, int quantite, double subtotal) {
        this(panierId, produitId, produitNom, prixUnitaire, quantite);
        this.id       = id;
        this.subtotal = subtotal;
    }

    // -------------------------------------------------------------------------
    // Méthodes métier
    // -------------------------------------------------------------------------

    public void recalculerSubtotal() {
        this.subtotal = this.prixUnitaire * this.quantite;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int getId()                       { return id; }
    public void setId(int id)                { this.id = id; }

    public int getPanierId()                 { return panierId; }
    public void setPanierId(int panierId)    { this.panierId = panierId; }

    public int getProduitId()                { return produitId; }
    public void setProduitId(int produitId)  { this.produitId = produitId; }

    public String getProduitNom()            { return produitNom; }
    public void setProduitNom(String n)      { this.produitNom = n; }

    public double getPrixUnitaire()          { return prixUnitaire; }
    public void setPrixUnitaire(double p)    { this.prixUnitaire = p; }

    public int getQuantite()                 { return quantite; }
    public void setQuantite(int quantite)    { this.quantite = quantite; }

    public double getSubtotal()              { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    @Override
    public String toString() {
        return "LignePanier{produit='" + produitNom + "', qté=" + quantite
                + ", prix=" + prixUnitaire + ", subtotal=" + subtotal + "}";
    }
}