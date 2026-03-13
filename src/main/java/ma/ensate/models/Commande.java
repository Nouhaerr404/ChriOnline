package ma.ensate.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Commande implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String VALIDE     = "VALIDE";
    public static final String EXPEDIE    = "EXPEDIE";
    public static final String LIVRE      = "LIVRE";

    private String id;
    private Utilisateur utilisateur;
    private List<LigneCommande> lignes;
    private LocalDateTime commandeDate;
    private String statut;
    private double prixAPayer;

    public Commande() {
        this.id = UUID.randomUUID().toString();
        this.lignes = new ArrayList<>();
        this.commandeDate = LocalDateTime.now();
        this.statut = EN_ATTENTE;
    }

    public Commande(Utilisateur utilisateur) {
        this();
        this.utilisateur = utilisateur;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) {
        this.lignes = lignes;
        calculerTotal();
    }

    public LocalDateTime getCommandeDate() { return commandeDate; }
    public void setCommandeDate(LocalDateTime commandeDate) { this.commandeDate = commandeDate; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public double getPrixAPayer() { return prixAPayer; }
    public void setPrixAPayer(double prixAPayer) { this.prixAPayer = prixAPayer; }

    public void calculerTotal() {
        this.prixAPayer = lignes.stream()
                .mapToDouble(LigneCommande::getPrixLigne)
                .sum();
    }
}