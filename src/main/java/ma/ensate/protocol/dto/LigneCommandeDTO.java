package ma.ensate.protocol.dto;

import java.io.Serializable;

/**
 * DTO pour une ligne de commande dans les requêtes
 */
public class LigneCommandeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int produitId;
    private int quantite;

    public LigneCommandeDTO() {}

    public LigneCommandeDTO(int produitId, int quantite) {
        this.produitId = produitId;
        this.quantite = quantite;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }
}

