package ma.ensate.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Paiement implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String commandeId;
    private LocalDateTime datePayment;
    private MethodePaiement methodePayment;
    private StatutPaiement statutPayment;
    private double prixAPayer;
    private String cardLast4;

    public Paiement() {
        this.id = UUID.randomUUID().toString();
        this.datePayment = LocalDateTime.now();
        this.statutPayment = StatutPaiement.EN_ATTENTE;
    }


    public Paiement(String commandeId, MethodePaiement methodePayment, double prixAPayer) {
        this();
        this.commandeId = commandeId;
        this.methodePayment = methodePayment;
        this.prixAPayer = prixAPayer;
    }

    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }

    public String getCommandeId() { 
        return commandeId; 
    }
    
    public void setCommandeId(String commandeId) { 
        this.commandeId = commandeId; 
    }

    public LocalDateTime getDatePayment() { 
        return datePayment; 
    }
    
    public void setDatePayment(LocalDateTime datePayment) { 
        this.datePayment = datePayment; 
    }

    public MethodePaiement getMethodePayment() { 
        return methodePayment; 
    }
    
    public void setMethodePayment(MethodePaiement methodePayment) { 
        this.methodePayment = methodePayment; 
    }

    public StatutPaiement getStatutPayment() { 
        return statutPayment; 
    }
    
    public void setStatutPayment(StatutPaiement statutPayment) { 
        this.statutPayment = statutPayment; 
    }

    public double getPrixAPayer() { 
        return prixAPayer; 
    }
    
    public void setPrixAPayer(double prixAPayer) { 
        this.prixAPayer = prixAPayer; 
    }

    public String getCardLast4() { 
        return cardLast4; 
    }
    
    public void setCardLast4(String cardLast4) { 
        this.cardLast4 = cardLast4; 
    }


    public void accepter() {
        this.statutPayment = StatutPaiement.ACCEPTE;
    }

    public void refuser() {
        this.statutPayment = StatutPaiement.REFUSE;
    }
}