package ma.ensate.protocol.dto;

import java.io.Serializable;

/**
 * DTO pour la requête de paiement
 */
public class PaiementRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String commandeId;
    private String methodePaiement; // "ALIVRAISON" ou "CARTE_BANCAIRE"
    private String cardLast4; // Optionnel, seulement pour CARTE_BANCAIRE

    public PaiementRequest() {}

    public PaiementRequest(String commandeId, String methodePaiement, String cardLast4) {
        this.commandeId = commandeId;
        this.methodePaiement = methodePaiement;
        this.cardLast4 = cardLast4;
    }

    public String getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(String commandeId) {
        this.commandeId = commandeId;
    }

    public String getMethodePaiement() {
        return methodePaiement;
    }

    public void setMethodePaiement(String methodePaiement) {
        this.methodePaiement = methodePaiement;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }
}

