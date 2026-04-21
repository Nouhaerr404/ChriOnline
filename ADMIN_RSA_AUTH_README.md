# Authentification Admin par Challenge-Response RSA

Ce document décrit l'implémentation du système d'authentification admin basé sur la cryptographie asymétrique RSA et un mécanisme challenge-response.

## Vue d'ensemble

Le système permet aux administrateurs de s'authentifier sans mot de passe en utilisant une paire de clés RSA :
- **Clé publique** : stockée côté serveur dans la base de données
- **Clé privée** : stockée côté admin (sécurisée)

## Principe de fonctionnement

1. L'admin demande l'accès via son email
2. Le serveur génère un challenge aléatoire (32 bytes)
3. L'admin signe le challenge avec sa clé privée
4. Le serveur vérifie la signature avec la clé publique
5. Si valide → accès autorisé au dashboard admin

## Architecture

### Composants côté serveur

- `ma.ensate.security.RSAKeyPairGenerator` : Génération de paires de clés RSA
- `ma.ensate.security.ChallengeGenerator` : Génération de challenges aléatoires
- `ma.ensate.security.RSASigner` : Signature de challenges (client)
- `ma.ensate.security.RSAVerifier` : Vérification de signatures (serveur)
- `ma.ensate.security.KeySerializer` : Sérialisation/désérialisation des clés
- `ma.ensate.security.AdminAuthService` : Service de gestion des challenges
- `ma.ensate.server.services.UserService` : Endpoints d'authentification admin
- `ma.ensate.server.network.ClientHandler` : Routage des requêtes

### Composants côté client

- `ma.ensate.client.security.AdminAuthClient` : Utilitaire d'authentification client
- `ma.ensate.client.views.AdminLoginView` : Interface de connexion admin

### Modèles de données

- `ma.ensate.models.Administrateur` : Étendu avec champ `publicKey`
- Table `utilisateur` : Ajout de colonne `public_key` (TEXT)

## Installation

### 1. Mise à jour de la base de données

Exécutez le script SQL pour ajouter la colonne de clé publique :

```bash
mysql -u votre_user -p votre_database < database_admin_auth.sql
```

Ou manuellement :

```sql
ALTER TABLE utilisateur 
ADD COLUMN public_key TEXT NULL 
COMMENT 'Clé publique RSA pour authentification challenge-response (admins seulement)';

CREATE INDEX idx_utilisateur_email ON utilisateur(email);
CREATE INDEX idx_utilisateur_type_compte ON utilisateur(type_compte);
```

### 2. Génération des clés pour un administrateur

Exécutez la classe `AdminKeyGenerator` pour générer une paire de clés :

```bash
cd src/main/java
java ma.ensate.test.AdminKeyGenerator
```

Cela va :
- Générer une paire de clés RSA 2048 bits
- Afficher la clé publique (à copier)
- Sauvegarder la clé privée dans `admin_private_key.pem`

### 3. Association de la clé publique à l'admin

Exécutez la requête SQL pour associer la clé publique à un compte admin existant :

```sql
UPDATE utilisateur 
SET public_key = 'VOTRE_CLE_PUBLIQUE_ICI' 
WHERE email = 'admin@example.com' AND type_compte = 'ADMINISTRATEUR';
```

### 4. Distribution de la clé privée

- Transférez le fichier `admin_private_key.pem` à l'admin de manière sécurisée
- L'admin doit stocker ce fichier dans un emplacement sécurisé
- **NE JAMAIS PARTAGER LA CLÉ PRIVÉE**

## Utilisation

### Pour l'administrateur

1. Lancer l'application client
2. Cliquer sur "Connexion Admin" (bouton à ajouter dans LoginView)
3. Entrer l'email admin
4. Sélectionner le fichier de clé privée (`admin_private_key.pem`)
5. Cliquer sur "Se connecter"

Le système va :
- Demander un challenge au serveur
- Signer le challenge avec la clé privée
- Envoyer la signature au serveur
- Le serveur vérifie et autorise l'accès si valide

## Sécurité

### Avantages

- **Aucun mot de passe stocké** : Élimine les risques de fuite de mots de passe
- **Protection contre phishing** : La clé privée ne peut être extraite par un site malveillant
- **Challenge unique** : Chaque authentification utilise un challenge différent (anti-replay)
- **Expiration des challenges** : Les challenges expirent après 30 secondes
- **Niveau sécurité élevé** : Comparable à SSH

### Mesures de sécurité

1. **Protection de la clé privée** :
   - Stocker la clé privée dans un emplacement sécurisé
   - Utiliser des permissions restreintes sur le fichier
   - Idéalement, utiliser un HSM (Hardware Security Module)

2. **Validation IP** :
   - L'accès admin est restreint aux adresses IP internes
   - Vérification dans `UserService.verifierAccesIPAdmin()`

3. **Anti-replay** :
   - Les challenges sont consommés après utilisation
   - Expiration automatique après 30 secondes

### Améliorations possibles

- Utiliser ECDSA au lieu de RSA (plus performant)
- Ajouter TLS pour chiffrer la communication
- Implémenter la double authentification (2FA)
- Utiliser un keystore sécurisé pour les clés
- Ajouter une rotation périodique des clés

## API

### Endpoints serveur

#### GENERATE_CHALLENGE_ADMIN

Génère un challenge pour l'authentification admin.

**Requête** :
```java
Request("GENERATE_CHALLENGE_ADMIN", "admin@example.com")
```

**Réponse** :
```java
Response(true, "Challenge généré.", "challenge_base64")
```

#### VERIFY_SIGNATURE_ADMIN

Vérifie la signature du challenge.

**Requête** :
```java
Request("VERIFY_SIGNATURE_ADMIN", 
    new Object[]{"admin@example.com", "challenge_base64", "signature_base64"})
```

**Réponse** :
```java
Response(true, "Authentification réussie !", administrateur)
```

### Méthodes client

```java
// Demander un challenge
String challenge = AdminAuthClient.requestChallenge("admin@example.com");

// Authentifier avec challenge
boolean success = AdminAuthClient.authenticateWithChallenge(
    "admin@example.com", challenge, privateKey);

// Authentification complète
boolean success = AdminAuthClient.authenticate("admin@example.com", privateKey);
```

## Dépannage

### Erreur "Clé publique non configurée"

La clé publique n'a pas été associée au compte admin dans la base de données. Vérifiez avec :

```sql
SELECT email, public_key FROM utilisateur WHERE type_compte = 'ADMINISTRATEUR';
```

### Erreur "Signature invalide"

- Vérifiez que la clé privée correspond à la clé publique stockée
- Assurez-vous que le fichier de clé privée n'est pas corrompu
- Vérifiez que le challenge n'a pas expiré (30 secondes)

### Erreur "Challenge invalide ou expiré"

Le challenge a expiré (plus de 30 secondes) ou a déjà été utilisé. Réessayez.

## Fichiers créés/modifiés

### Nouveaux fichiers
- `src/main/java/ma/ensate/security/RSAKeyPairGenerator.java`
- `src/main/java/ma/ensate/security/ChallengeGenerator.java`
- `src/main/java/ma/ensate/security/RSASigner.java`
- `src/main/java/ma/ensate/security/RSAVerifier.java`
- `src/main/java/ma/ensate/security/KeySerializer.java`
- `src/main/java/ma/ensate/security/AdminAuthService.java`
- `src/main/java/ma/ensate/client/security/AdminAuthClient.java`
- `src/main/java/ma/ensate/client/views/AdminLoginView.java`
- `src/main/java/ma/ensate/test/AdminKeyGenerator.java`
- `database_admin_auth.sql`

### Fichiers modifiés
- `src/main/java/ma/ensate/models/Administrateur.java` (ajout champ publicKey)
- `src/main/java/ma/ensate/server/dao/UtilisateurDAO.java` (méthodes pour clé publique)
- `src/main/java/ma/ensate/server/services/UserService.java` (méthodes auth admin)
- `src/main/java/ma/ensate/server/network/ClientHandler.java` (endpoints admin)

## Conclusion

Le système challenge-response avec clé publique offre une authentification admin robuste, adaptée aux applications sécurisées modernes sans mot de passe. Cette implémentation suit les meilleures pratiques de sécurité cryptographique et peut être étendue avec des fonctionnalités supplémentaires selon les besoins.
