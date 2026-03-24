package ma.ensate.models;

import java.util.ArrayList;
import java.util.List;


//  Un Client possède exactement un Panier (relation 1-1).

public class Panier {

    private int id;
    private int clientId;
    private double total;
    private List<LignePanier> lignes;

    public Panier() {
        this.lignes = new ArrayList<>();
        this.total  = 0.0;
    }

    public Panier(int clientId) {
        this();
        this.clientId = clientId;
    }

    public Panier(int id, int clientId, double total) {
        this();
        this.id       = id;
        this.clientId = clientId;
        this.total    = total;
    }

    public void recalculerTotal() {
        double somme = 0.0;
        for (LignePanier ligne : lignes) {
            somme = somme + ligne.getSubtotal();
        }
        this.total = somme;
    }

    public void ajouterOuMettreAJourLigne(LignePanier nouvelleLigne) {
        for (LignePanier l : lignes) { //on parcourt toutes les lignes deja dans le panier
            if (l.getProduitId() == nouvelleLigne.getProduitId()) { // if we find une ligne avec the same product , it means its alrady dans le panier
                l.setQuantite(l.getQuantite() + nouvelleLigne.getQuantite());
                l.recalculerSubtotal();
                recalculerTotal();
                return;
            }
        }
        lignes.add(nouvelleLigne);
        recalculerTotal();
    }

    public void supprimerLigne(int produitId) {
        LignePanier aSupprimer = null;

        for (LignePanier l : lignes) {
            if (l.getProduitId() == produitId) {
                aSupprimer = l;
                break;
            }
        }

        if (aSupprimer != null) {
            lignes.remove(aSupprimer);
        }

        recalculerTotal();
    }

    public boolean estVide() {
        return lignes == null || lignes.isEmpty();
    } // true if au mois le panier contient un article

    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }

    public int getClientId()              { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public double getTotal()              { return total; }
    public void setTotal(double total)    { this.total = total; }

    public List<LignePanier> getLignes()              { return lignes; }
    public void setLignes(List<LignePanier> lignes)   {
        this.lignes = lignes;
        recalculerTotal();
    }

    @Override
    public String toString() {
        return "Panier{id=" + id + ", clientId=" + clientId
                + ", total=" + total + ", lignes=" + lignes.size() + "}";
    }
}