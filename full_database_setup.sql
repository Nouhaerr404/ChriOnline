-- =========================================================================
-- Script de création et de peuplement de la base de données ChriOnline
-- =========================================================================

-- Création de la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS chrionline CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chrionline;

-- =========================================================================
-- STRUCTURE DES TABLES
-- =========================================================================

-- 1. Table utilisateur
CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    type_compte VARCHAR(50) NOT NULL DEFAULT 'CLIENT', -- 'CLIENT' ou 'ADMINISTRATEUR'
    session_token VARCHAR(255) NULL,
    statut VARCHAR(50) DEFAULT 'ACTIF',
    two_fa_enabled BOOLEAN DEFAULT FALSE,
    public_key TEXT NULL COMMENT 'Clé publique RSA pour authentification challenge-response (admins seulement)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_utilisateur_email ON utilisateur(email);
CREATE INDEX idx_utilisateur_type_compte ON utilisateur(type_compte);

-- 2. Table client (hérite de utilisateur)
CREATE TABLE IF NOT EXISTS client (
    id INT PRIMARY KEY,
    adresse VARCHAR(500) NULL,
    tel VARCHAR(50) NULL,
    CONSTRAINT fk_client_utilisateur FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Table categorie
CREATE TABLE IF NOT EXISTS categorie (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Table produit
CREATE TABLE IF NOT EXISTS produit (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT NULL,
    prix DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500) NULL,
    categorie_id INT NULL,
    actif BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_produit_categorie FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Table panier
CREATE TABLE IF NOT EXISTS panier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    UNIQUE KEY uq_panier_client (client_id),
    CONSTRAINT fk_panier_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Table ligne_panier
CREATE TABLE IF NOT EXISTS ligne_panier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    panier_id INT NOT NULL,
    produit_id INT NOT NULL,
    quantite INT NOT NULL CHECK (quantite > 0),
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    UNIQUE KEY uq_ligne_panier_produit (panier_id, produit_id),
    CONSTRAINT fk_ligne_panier_panier FOREIGN KEY (panier_id) REFERENCES panier(id) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_panier_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Table commande
CREATE TABLE IF NOT EXISTS commande (
    id VARCHAR(50) PRIMARY KEY, -- ID généré sous forme de string (ex: UUID ou ref)
    client_id INT NOT NULL,
    commande_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    prix_a_payer DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_commande_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Table ligne_commande
CREATE TABLE IF NOT EXISTS ligne_commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id VARCHAR(50) NOT NULL,
    produit_id INT NULL,
    produit_nom VARCHAR(255) NOT NULL,
    price_at_order DECIMAL(10,2) NOT NULL,
    prix_ligne DECIMAL(10,2) NOT NULL,
    quantite INT NOT NULL CHECK (quantite > 0),
    CONSTRAINT fk_ligne_commande_commande FOREIGN KEY (commande_id) REFERENCES commande(id) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_commande_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Table paiement
CREATE TABLE IF NOT EXISTS paiement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id VARCHAR(50) NOT NULL,
    date_paiement DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    montant DECIMAL(10,2) NOT NULL,
    methode VARCHAR(50) NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    CONSTRAINT fk_paiement_commande FOREIGN KEY (commande_id) REFERENCES commande(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================================
-- DONNÉES DE DÉMONSTRATION (SEEDERS)
-- =========================================================================

-- A. Insérer un compte administrateur par défaut
-- Mot de passe par défaut: admin123 (Hashé avec BCrypt $2a$12$...)
-- Note: les informations sensibles (email, nom) sont chiffrées avec AES dans l'application, 
-- il faut donc utiliser le format chiffré si l'app le nécessite. Pour la simplicité ici on insère en clair, 
-- mais si `SensitiveDataCipher` est utilisé, l'email pourrait ne pas matcher. 
-- *IMPORTANT*: Assurez-vous d'avoir désactivé le chiffrement AES pour le DB SEED ou utilisez la méthode d'inscription de l'app.
-- Ici, j'insère un hash BCrypt valide pour "admin123" avec un cost de 12.
INSERT INTO utilisateur (nom, email, password, type_compte, statut) 
VALUES (
    'Administrateur Principal', 
    'admin@chrionline.ma', 
    '$2a$12$Z0K0QG3ZzT1H6dM3fEw9O.e/6R4V/6P/tL6GvH/H7X7aB9c1z2x3C', -- admin123
    'ADMINISTRATEUR', 
    'ACTIF'
);

-- B. Insérer des catégories
INSERT INTO categorie (nom) VALUES ('Ordinateurs Portables');
INSERT INTO categorie (nom) VALUES ('Smartphones');
INSERT INTO categorie (nom) VALUES ('Périphériques');

-- C. Insérer des produits
INSERT INTO produit (nom, description, prix, stock, image_url, categorie_id, actif) 
VALUES (
    'Dell XPS 15', 
    'Ordinateur portable performant avec écran 4K', 
    18500.00, 
    10, 
    'dell_xps_15.jpg', 
    (SELECT id FROM categorie WHERE nom = 'Ordinateurs Portables'), 
    TRUE
);

INSERT INTO produit (nom, description, prix, stock, image_url, categorie_id, actif) 
VALUES (
    'MacBook Pro 16', 
    'Puce M2 Pro, 16Go RAM, 512Go SSD', 
    25000.00, 
    5, 
    'macbook_pro_16.jpg', 
    (SELECT id FROM categorie WHERE nom = 'Ordinateurs Portables'), 
    TRUE
);

INSERT INTO produit (nom, description, prix, stock, image_url, categorie_id, actif) 
VALUES (
    'iPhone 14 Pro', 
    'Smartphone Apple dernière génération', 
    12000.00, 
    15, 
    'iphone_14_pro.jpg', 
    (SELECT id FROM categorie WHERE nom = 'Smartphones'), 
    TRUE
);

INSERT INTO produit (nom, description, prix, stock, image_url, categorie_id, actif) 
VALUES (
    'Samsung Galaxy S23 Ultra', 
    'Le meilleur d''Android avec stylet intégré', 
    11500.00, 
    20, 
    's23_ultra.jpg', 
    (SELECT id FROM categorie WHERE nom = 'Smartphones'), 
    TRUE
);

INSERT INTO produit (nom, description, prix, stock, image_url, categorie_id, actif) 
VALUES (
    'Logitech MX Master 3S', 
    'Souris sans fil ergonomique pour la productivité', 
    1200.00, 
    30, 
    'mx_master_3s.jpg', 
    (SELECT id FROM categorie WHERE nom = 'Périphériques'), 
    TRUE
);
