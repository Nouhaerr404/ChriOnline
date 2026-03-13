package ma.ensate.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Paiement implements Serializable {
    private static final long serialVersionUID = 1L;

    // Méthodes de paiement
    public static final String ALIVRAISON     = "ALIVRAISON";
    public static final String CARTE_BANCAIRE = "CARTE_BANCAIRE";

    // Statuts
    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String ACCEPTE    = "ACCEPTE";
    public static final String REFUSE     = "REFUSE";

    private String id;
    private String commandeId;
    private LocalDateTime datePayment;
    private String methodePayment;
    private String statutPayment;
    private double prixAPayer;
    private String cardLast4;

    public Paiement() {
        this.id = UUID.randomUUID().toString();
        this.datePayment = LocalDateTime.now();
        this.statutPayment = EN_ATTENTE;
    }

    public Paiement(String commandeId, String methodePayment, double prixAPayer) {
        this();
        this.commandeId = commandeId;
        this.methodePayment = methodePayment;
        this.prixAPayer = prixAPayer;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCommandeId() { return commandeId; }
    public void setCommandeId(String commandeId) { this.commandeId = commandeId; }

    public LocalDateTime getDatePayment() { return datePayment; }
    public void setDatePayment(LocalDateTime datePayment) { this.datePayment = datePayment; }

    public String getMethodePayment() { return methodePayment; }
    public void setMethodePayment(String methodePayment) { this.methodePayment = methodePayment; }

    public String getStatutPayment() { return statutPayment; }
    public void setStatutPayment(String statutPayment) { this.statutPayment = statutPayment; }

    public double getPrixAPayer() { return prixAPayer; }
    public void setPrixAPayer(double prixAPayer) { this.prixAPayer = prixAPayer; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
}