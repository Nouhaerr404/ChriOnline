package ma.ensate.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Commande implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Client client;
    private List<LigneCommande> lignes;
    private LocalDateTime commandeDate;
    private StatutCommande statut;
    private double prixAPayer;
    private Paiement paiement;

    public Commande() {
        this.id = UUID.randomUUID().toString();
        this.lignes = new ArrayList<>();
        this.commandeDate = LocalDateTime.now();
        this.statut = StatutCommande.EN_ATTENTE;
    }

    public Commande(Client client) {
        this();
        this.client = client;
    }

    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }

    public Client getClient() { 
        return client; 
    }
    
    public void setClient(Client client) { 
        this.client = client; 
    }

    public List<LigneCommande> getLignes() { 
        return lignes; 
    }
    
    public void setLignes(List<LigneCommande> lignes) {
        this.lignes = lignes;
        calculerTotal();
    }

    public LocalDateTime getCommandeDate() { 
        return commandeDate; 
    }
    
    public void setCommandeDate(LocalDateTime commandeDate) { 
        this.commandeDate = commandeDate; 
    }

    public StatutCommande getStatut() { 
        return statut; 
    }
    
    public void setStatut(StatutCommande statut) { 
        this.statut = statut; 
    }

    public double getPrixAPayer() { 
        return prixAPayer; 
    }
    
    public void setPrixAPayer(double prixAPayer) { 
        this.prixAPayer = prixAPayer; 
    }

    public Paiement getPaiement() { 
        return paiement; 
    }
    
    public void setPaiement(Paiement paiement) { 
        this.paiement = paiement; 
    }

    public void calculerTotal() {
        if (lignes != null && !lignes.isEmpty()) {
            this.prixAPayer = lignes.stream()
                    .mapToDouble(LigneCommande::getPrixLigne)
                    .sum();
        } else {
            this.prixAPayer = 0.0;
        }
    }
}