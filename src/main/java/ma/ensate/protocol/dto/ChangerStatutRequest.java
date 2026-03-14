package ma.ensate.protocol.dto;

import java.io.Serializable;

/**
 * DTO pour la requête de changement de statut d'une commande
 */
public class ChangerStatutRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String commandeId;
    private String nouveauStatut; // "EN_ATTENTE", "VALIDE", "EXPEDIE", "LIVRE"

    public ChangerStatutRequest() {}

    public ChangerStatutRequest(String commandeId, String nouveauStatut) {
        this.commandeId = commandeId;
        this.nouveauStatut = nouveauStatut;
    }

    public String getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(String commandeId) {
        this.commandeId = commandeId;
    }

    public String getNouveauStatut() {
        return nouveauStatut;
    }

    public void setNouveauStatut(String nouveauStatut) {
        this.nouveauStatut = nouveauStatut;
    }
}

