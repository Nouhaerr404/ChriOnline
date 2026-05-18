# Mini-Projet 2: Répartition des Tâches (4 personnes)

## Vue d'ensemble

Projet: Sécurisation de l'application e-commerce ChriOnline avec AES et RSA (inspiré du protocole HTTPS)

---

## Personne 1: Implémentation RSA & Gestion des Clés

### Responsabilités

**Génération et gestion des clés RSA**
- Créer une classe `RSAKeyManager` pour générer des paires de clés RSA (2048 bits)
- Implémenter la génération de clés côté serveur
- Sérialisation/désérialisation des clés (Base64, PEM)
- Stockage sécurisé de la clé privée serveur

**Chiffrement/Déchiffrement RSA**
- Créer une classe `RSAEncryptor` pour le chiffrement asymétrique
- Méthodes: `encrypt(data, publicKey)` et `decrypt(encryptedData, privateKey)`
- Gestion des exceptions et validation des données

**Distribution de la clé publique**
- Endpoint serveur pour envoyer la clé publique RSA au client
- Validation de la clé publique reçue par le client

### Livrables
- `src/main/java/ma/ensate/security/RSAKeyManager.java`
- `src/main/java/ma/ensate/security/RSAEncryptor.java`
- Tests unitaires pour le chiffrement RSA

### Dépendances
- Aucune (tâche indépendante)

### Estimation temps
- 6-8 heures

---

## Personne 2: Implémentation AES & Chiffrement des Données

### Responsabilités

**Génération de clés AES**
- Créer une classe `AESKeyGenerator` pour générer des clés AES (256 bits)
- Génération sécurisée avec `SecureRandom`

**Chiffrement/Déchiffrement AES**
- Créer une classe `AESEncryptor` pour le chiffrement symétrique
- Implémentation en mode CBC ou GCM (recommandé: GCM pour authentification)
- Génération et gestion des IV (Initialization Vector) uniques
- Méthodes: `encrypt(data, key, iv)` et `decrypt(encryptedData, key, iv)`

**Gestion des IV**
- Génération d'IV unique pour chaque chiffrement
- Stockage/transmission de l'IV avec les données chiffrées
- Validation de l'IV côté récepteur

### Livrables
- `src/main/java/ma/ensate/security/AESKeyGenerator.java`
- `src/main/java/ma/ensate/security/AESEncryptor.java`
- Tests unitaires pour le chiffrement AES

### Dépendances
- Aucune (tâche indépendante)

### Estimation temps
- 6-8 heures

---

## Personne 3: Protocole Sécurisé (Handshake HTTPS-like)

### Responsabilités

**Implémentation du handshake**
- Créer une classe `SecureHandshake` pour gérer le protocole d'échange de clés
- Scénario:
  1. Client demande la clé publique RSA au serveur
  2. Serveur envoie sa clé publique
  3. Client génère une clé AES aléatoire
  4. Client chiffre la clé AES avec RSA
  5. Client envoie la clé AES chiffrée au serveur
  6. Serveur déchiffre la clé AES avec sa clé privée
  7. Établissement du canal sécurisé

**Wrapper de communication sécurisée**
- Créer une classe `SecureChannel` pour encapsuler les requêtes/réponses
- Chiffrement automatique des données envoyées
- Déchiffrement automatique des données reçues
- Gestion des erreurs de chiffrement/déchiffrement

**Protection contre rejeu**
- Ajout de timestamp/nonce dans les messages
- Validation de la fraîcheur des messages

### Livrables
- `src/main/java/ma/ensate/security/SecureHandshake.java`
- `src/main/java/ma/ensate/security/SecureChannel.java`
- Documentation du protocole de handshake

### Dépendances
- Tâche 1 (RSA)
- Tâche 2 (AES)

### Estimation temps
- 8-10 heures

---

## Personne 4: Intégration & Sécurisation du Stockage

### Responsabilités

**Intégration client-serveur**
- Modifier `ClientTCP` pour utiliser `SecureChannel`
- Modifier `ClientHandler` pour utiliser `SecureChannel`
- Intégrer le handshake au démarrage de la connexion
- Assurer la compatibilité avec l'authentification admin existante

**Sécurisation des données stockées**
- Identifier les données sensibles dans la base de données (mots de passe, emails, etc.)
- Implémenter le chiffrement des données avant stockage
- Implémenter le déchiffrement après récupération
- Mise à jour de `UtilisateurDAO` pour gérer les données chiffrées

**Tests de sécurité**
- Tests d'intégration de la communication sécurisée
- Tests de protection contre interception
- Tests de protection contre rejeu
- Validation de l'intégrité des données

**Documentation et démonstration**
- Documenter l'architecture de sécurité
- Préparer la démonstration pour l'entretien individuel

### Livrables
- Modifications de `ClientTCP.java` et `ClientHandler.java`
- Modifications de `UtilisateurDAO.java` pour données chiffrées
- Tests de sécurité
- Documentation finale
- Script de démonstration

### Dépendances
- Tâche 1 (RSA)
- Tâche 2 (AES)
- Tâche 3 (Protocole)

### Estimation temps
- 8-10 heures

---

## Chronologie Suggérée

### Semaine 1
- **Personnes 1 & 2**: Implémentation RSA et AES (tâches parallèles)
- **Personne 3**: Étude du protocole et préparation
- **Personne 4**: Analyse des données sensibles

### Semaine 2
- **Personne 3**: Implémentation du handshake (après tâches 1 & 2)
- **Personne 4**: Début de l'intégration
- **Personnes 1 & 2**: Tests et corrections

### Semaine 3
- **Personne 4**: Intégration complète et tests de sécurité
- **Tous**: Révision et préparation de la démonstration

---

## Coordination

### Réunions recommandées
1. **Jour 1**: Kick-off du projet, répartition détaillée
2. **Fin semaine 1**: Validation des implémentations RSA et AES
3. **Milieu semaine 2**: Validation du handshake
4. **Fin semaine 2**: Point sur l'intégration
5. **Fin semaine 3**: Revue finale et préparation démonstration

### Outils de collaboration
- Git pour le versioning (branches par personne)
- Documentation partagée (README.md)
- Tests unitaires partagés

---

## Critères de Succès

- ✅ Communication client-serveur entièrement chiffrée
- ✅ Échange de clés sécurisé (RSA + AES)
- ✅ Protection contre interception
- ✅ Protection contre rejeu
- ✅ Données sensibles chiffrées en base de données
- ✅ Tests de sécurité validés
- ✅ Démonstration fonctionnelle

---

## Notes Importantes

- Chaque personne doit documenter son code
- Les tests unitaires sont obligatoires pour chaque composant
- Utiliser les meilleures pratiques de sécurité (pas de hardcoding de clés)
- Respecter les contraintes: AES CBC/GCM, IV unique
- Communiquer régulièrement pour éviter les blocages
