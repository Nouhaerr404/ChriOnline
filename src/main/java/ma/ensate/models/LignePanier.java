package ma.ensate.models;

import java.io.Serializable;

public class LignePanier implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Produit produit;
    private int quantite;
    private double subtotal;

    public LignePanier() {}

    public LignePanier(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = quantite;
        this.subtotal = produit.getPrix() * quantite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) {
        this.quantite = quantite;
        if (this.produit != null)
            this.subtotal = this.produit.getPrix() * quantite;
    }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}