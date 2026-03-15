package ma.ensate.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle Panier — correspond à la table `panier` en base.
 * Un Client possède exactement un Panier (relation 1-1).
 */
public class Panier {

    private int id;
    private int clientId;
    private double total;
    private List<LignePanier> lignes;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Méthodes métier
    // -------------------------------------------------------------------------

    /**
     * Recalcule le total à partir des lignes chargées en mémoire.
     * Doit être appelé après toute modification des lignes.
     */
    public void recalculerTotal() {
        this.total = lignes.stream()
                .mapToDouble(LignePanier::getSubtotal)
                .sum();
    }

    /**
     * Ajoute ou met à jour une ligne dans la liste locale.
     * N'interagit PAS avec la base de données.
     */
    public void ajouterOuMettreAJourLigne(LignePanier nouvelleLigne) {
        for (LignePanier l : lignes) {
            if (l.getProduitId() == nouvelleLigne.getProduitId()) {
                l.setQuantite(l.getQuantite() + nouvelleLigne.getQuantite());
                l.recalculerSubtotal();
                recalculerTotal();
                return;
            }
        }
        lignes.add(nouvelleLigne);
        recalculerTotal();
    }

    /**
     * Supprime une ligne par produitId dans la liste locale.
     */
    public void supprimerLigne(int produitId) {
        lignes.removeIf(l -> l.getProduitId() == produitId);
        recalculerTotal();
    }

    public boolean estVide() {
        return lignes == null || lignes.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

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