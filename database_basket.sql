-- PROJET CHRIONLINE
-- Table panier
CREATE TABLE IF NOT EXISTS panier (
    id         INT           NOT NULL AUTO_INCREMENT,
    client_id  INT           NOT NULL,       -- FK vers utilisateur(id)
    total      DECIMAL(10,2) NOT NULL DEFAULT 0.00,

    PRIMARY KEY (id),
    UNIQUE KEY uq_panier_client (client_id),   -- 1 panier max par client
    CONSTRAINT fk_panier_client
        FOREIGN KEY (client_id) REFERENCES utilisateur(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table ligne_panier
CREATE TABLE IF NOT EXISTS ligne_panier (
    id          INT           NOT NULL AUTO_INCREMENT,
    panier_id   INT           NOT NULL,       -- FK vers panier(id)
    produit_id  INT           NOT NULL,       -- FK vers produit(id)
    quantite    INT           NOT NULL CHECK (quantite > 0),
    subtotal    DECIMAL(10,2) NOT NULL DEFAULT 0.00,  -- prix × quantité

    PRIMARY KEY (id),
    UNIQUE KEY uq_ligne_panier_produit (panier_id, produit_id),
    CONSTRAINT fk_ligne_panier_panier
        FOREIGN KEY (panier_id)  REFERENCES panier(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ligne_panier_produit
        FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
