-- PROJET CHRIONLINE
-- Mise à jour de la table utilisateur pour supporter l'authentification admin par challenge-response RSA

-- Ajout de la colonne pour stocker la clé publique RSA des administrateurs
ALTER TABLE utilisateur 
ADD COLUMN public_key TEXT NULL 
COMMENT 'Clé publique RSA pour authentification challenge-response (admins seulement)';

-- Index pour accélérer les recherches par email (si pas déjà existant)
CREATE INDEX idx_utilisateur_email ON utilisateur(email);

-- Index pour accélérer les recherches par type de compte
CREATE INDEX idx_utilisateur_type_compte ON utilisateur(type_compte);
