package ma.ensate.models;

public class LignePanier {

    private int    id;
    private int    panierId;
    private int    produitId;
    private String produitNom;
    private double prixUnitaire;
    private int    quantite; //dima >0
    private double subtotal;       // prixUnitaire × quantite

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public LignePanier() {}

    public LignePanier(int panierId, int produitId, String produitNom,
                       double prixUnitaire, int quantite) {
        this.panierId      = panierId;
        this.produitId     = produitId;
        this.produitNom    = produitNom;
        this.prixUnitaire  = prixUnitaire;
        this.quantite      = quantite;
        this.subtotal      = prixUnitaire * quantite;
    }
    //pour le rechargement apres que la ligne soit deja stocke dan sla bdd
    public LignePanier(int id, int panierId, int produitId, String produitNom,
                       double prixUnitaire, int quantite, double subtotal) {
        this(panierId, produitId, produitNom, prixUnitaire, quantite);
        this.id       = id;
        this.subtotal = subtotal;
    }

    public void recalculerSubtotal() {

        this.subtotal = this.prixUnitaire * this.quantite;
    }

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