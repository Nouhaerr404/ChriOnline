package ma.ensate.models;

import java.io.Serializable;

public class LigneCommande implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String commandeId;
    private Produit produit;
    private String produitNom;
    private double priceAtOrder;
    private double prixLigne;
    private int quantite;

    public LigneCommande() {}

    public LigneCommande(Produit produit, int quantite) {
        this.produit = produit;
        this.produitNom = produit.getNom();
        this.priceAtOrder = produit.getPrix();
        this.quantite = quantite;
        this.prixLigne = priceAtOrder * quantite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCommandeId() { return commandeId; }
    public void setCommandeId(String commandeId) {
        this.commandeId = commandeId;
        this.prixLigne = this.priceAtOrder * quantite;
    }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public double getPriceAtOrder() { return priceAtOrder; }
    public void setPriceAtOrder(double priceAtOrder) { this.priceAtOrder = priceAtOrder; }

    public double getPrixLigne() { return prixLigne; }
    public void setPrixLigne(double prixLigne) { this.prixLigne = prixLigne; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
}