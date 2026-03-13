package ma.ensate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Panier implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int clientId;
    private List<LignePanier> lignes;
    private double total;

    public Panier() {
        this.lignes = new ArrayList<>();
        this.total = 0.0;
    }

    public Panier(int clientId) {
        this.clientId = clientId;
        this.lignes = new ArrayList<>();
        this.total = 0.0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public List<LignePanier> getLignes() { return lignes; }
    public void setLignes(List<LignePanier> lignes) {
        this.lignes = lignes;
        calculerTotal();
    }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public void calculerTotal() {
        this.total = lignes.stream()
                .mapToDouble(LignePanier::getSubtotal)
                .sum();
    }
}