package ma.ensate.protocol.dto;

import ma.ensate.models.LigneCommande;

import java.io.Serializable;
import java.util.List;

/**
 * DTO pour la requête de création d'une commande
 */
public class CreerCommandeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int clientId;
    private List<LigneCommandeDTO> lignes;

    public CreerCommandeRequest() {}

    public CreerCommandeRequest(int clientId, List<LigneCommandeDTO> lignes) {
        this.clientId = clientId;
        this.lignes = lignes;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public List<LigneCommandeDTO> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneCommandeDTO> lignes) {
        this.lignes = lignes;
    }
}

