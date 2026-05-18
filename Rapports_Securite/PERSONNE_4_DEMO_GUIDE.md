# Guide de Démonstration - Personne 4

Ce guide explique comment démontrer les fonctionnalités de sécurité implémentées par la personne 4.

## 1. Démonstration du Canal Sécurisé (Interception)

### Objectif
Montrer que les communications entre le client et le serveur sont chiffrées et illisibles sans la clé AES.

### Étapes
1. Lancez le serveur `TCPServer`.
2. Lancez le client (Application JavaFX).
3. Observez les logs du serveur : vous verrez passer les phases du handshake RSA.
4. Une fois le canal établi, toutes les requêtes (LOGIN, GET_PRODUITS, etc.) sont chiffrées.
5. **Preuve** : Regardez le code de `SecureChannel.java`. Il préfixe chaque message avec un nonce et un timestamp avant de chiffrer avec AES-GCM.

## 2. Démonstration de l'Anti-Rejeu

### Objectif
Prouver qu'un pirate ne peut pas rejouer une transaction interceptée.

### Étapes
1. Exécutez le test JUnit `SecureChannelTest.shouldPreventReplayAttack`.
2. Ce test simule une interception :
    - Il capture les octets d'une requête chiffrée.
    - Il tente de renvoyer exactement les mêmes octets au serveur.
    - Le serveur lève une `SecurityException` avec le message "Rejeu détecté" car le nonce a déjà été utilisé.

## 3. Démonstration du Chiffrement au Stockage (At-Rest)

### Objectif
Montrer que les données sensibles sont chiffrées dans la base de données.

### Étapes
1. Inscrivez un nouvel utilisateur via l'interface.
2. Accédez à votre base de données (ex: via phpMyAdmin ou CLI).
3. Consultez la table `utilisateur` et la table `client`.
4. **Observation** :
    - Les champs `nom`, `email` dans `utilisateur` commencent par `ENC::`.
    - Les champs `adresse`, `tel` dans `client` commencent par `ENC::`.
    - Ils sont suivis d'une chaîne Base64 (l'IV + le ciphertext).
5. Connectez-vous avec cet utilisateur : l'application déchiffre les données de manière transparente pour l'affichage (Profil, Panier, etc.).

## 4. Démonstration de la Rétrocompatibilité

### Objectif
Montrer que les anciens utilisateurs (non chiffrés) peuvent toujours se connecter.

### Étapes
1. Insérez manuellement un utilisateur avec un email et un nom en clair dans la base de données.
2. Essayez de vous connecter ou d'afficher son profil.
3. L'application détecte que la valeur ne commence pas par `ENC::` et la traite comme du texte clair.

---

## 💡 Architecture & FAQ

### Quel est le lien entre le travail de la Personne 2 (AES) et la Personne 4 ?
La **Personne 2** a fabriqué le "moteur" (la classe `AESEncryptor`). La **Personne 3** a construit la "carrosserie" (le protocole `SecureHandshake` et le wrapper `SecureChannel`). 
La **Personne 4** est celle qui a "branché" tout cela dans la voiture (l'application) :
- Sans la Personne 4, l'algorithme AES de la Personne 2 resterait un code isolé sans effet sur la sécurité réelle des échanges.
- C'est grâce à l'intégration faite par la Personne 4 que chaque clic dans l'application déclenche réellement un chiffrement AES.

### Comment l'application a-t-elle été modifiée exactement par la Personne 4 ?
L'intervention de la Personne 4 a transformé le flux de données à deux niveaux critiques :

1.  **Au niveau Réseau (`ClientTCP` & `ClientHandler`)** :
    - Avant : Les objets `Request` et `Response` circulaient en clair (sérialisation Java standard).
    - Après : Dès que la socket est ouverte, un Handshake RSA est forcé. Une fois terminé, le flux est "encapsulé" dans `SecureChannel`. Désormais, `ObjectOutputStream.writeObject()` n'est plus utilisé directement ; on passe par le canal qui chiffre les données avec la clé AES négociée.

2.  **Au niveau Persistance (`UtilisateurDAO` & `ClientDAO`)** :
    - Avant : Les méthodes `ps.setString(index, value)` envoyaient les données telles quelles à MySQL.
    - Après : Chaque donnée sensible (nom, email, etc.) passe par `SensitiveDataCipher.encrypt()` avant d'être envoyée à la base de données. À la lecture, `decrypt()` est appelé systématiquement.

3.  **Au niveau Sécurité Applicative** :
    - Ajout d'une couche de protection contre le **rejeu** : chaque message réseau contient désormais un identifiant unique (nonce) et une date d'expiration, vérifiés par le serveur avant traitement.
