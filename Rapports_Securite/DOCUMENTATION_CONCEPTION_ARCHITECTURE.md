# 🏗️ ChriOnline - Rapport de Conception, Architecture et Justification des Choix

Ce document présente l'architecture système, les choix de conception logicielle et les justifications technologiques et cryptographiques qui ont guidé le développement de la plateforme **ChriOnline**. Ce projet a été élaboré selon le principe de **Security by Design** (sécurité dès la conception) dans le cadre du module de sécurité informatique.

---

## 1. Choix du Modèle Architectural : Sockets TCP Persistants vs HTTP/REST

### Description de l'Architecture
L'application repose sur une architecture **Client-Serveur propriétaire** utilisant des sockets TCP bruts et un protocole de communication orienté objet sérialisé et chiffré, plutôt qu'une API web HTTP/REST classique.

```mermaid
graph TD
    subgraph Client Space (JavaFX)
        A[Client View] -->|UI Action| B[ClientTCP / SessionManager]
    end
    
    subgraph Cryptographic Channel
        B -->|1. Handshake RSA/AES| C[SecureChannel]
        B -->|2. Request AES-GCM| C
    end
    
    subgraph Server Space (Multi-threaded)
        C -->|3. Decrypted Payload| D[TCPServer / ClientHandler]
        D -->|4. Business Logic| E[UserService / CommandeService]
        E -->|5. Parameterized Query| F[(MySQL Database)]
    end
```

### Justification des Choix
1. **Contrôle Total de la Pile de Protocoles** : L'utilisation de sockets TCP nus permet de concevoir de bout en bout le protocole d'échange et de chiffrement, sans hériter des vulnérabilités ou des complexités liées aux serveurs web (comme les injections d'en-têtes HTTP, le Cross-Origin Resource Sharing (CORS) mal configuré, ou les vulnérabilités liées aux serveurs mandataires).
2. **Connexion Persistante d'Intégrité Forte** : Contrairement au protocole HTTP qui est par nature sans état (*stateless*), la connexion TCP persistante permet de maintenir un contexte de session cryptographique et réseau lié à un socket actif unique, rendant le détournement de session (*Session Hijacking*) extrêmement difficile sans rompre la liaison physique de la socket.
3. **Optimisation des Performances Cryptographiques** : L'établissement d'un tunnel sécurisé sur socket évite la surcharge (overhead) des négociations TLS répétées caractéristiques de HTTP sur de courtes connexions, préservant ainsi les ressources du serveur.

---

## 2. Analyse des Choix et Justifications Cryptographiques

### 2.1. Cryptographie Hybride : RSA + AES
Pour sécuriser le canal de communication, nous avons opté pour une approche **hybride** associant le chiffrement asymétrique (RSA) et symétrique (AES).

*   **Pourquoi pas uniquement du RSA ?**
    Le chiffrement RSA est extrêmement lourd en calcul mathématique (exponentiation modulaire sur de très grands nombres) et est limité par la taille du bloc de données qu'il peut chiffrer (pas plus de 245 octets pour une clé de 2048 bits avec padding PKCS#1). L'utiliser pour chiffrer tous les échanges métiers paralyserait le serveur.
*   **Pourquoi pas uniquement de l'AES ?**
    Le chiffrement AES requiert que les deux parties partagent la même clé secrète. Si cette clé est codée en dur dans le client ou transmise en clair sur le réseau, la sécurité s'effondre.
*   **La Solution Hybride** : Le RSA est uniquement utilisé pour la phase d'échange de clés (Handshake) afin de chiffrer la clé AES générée dynamiquement par le client. Ensuite, l'intégralité du trafic réseau utilise la clé AES, offrant la rapidité du chiffrement symétrique et la sécurité de distribution du chiffrement asymétrique.

---

### 2.2. Mode d'Opération AES : GCM (AEAD) vs CBC

| Critère | AES-CBC (Cipher Block Chaining) | AES-GCM (Galois/Counter Mode) | Choix & Justification |
| :--- | :--- | :--- | :--- |
| **Confidentialité** | Oui | Oui | **AES-GCM** |
| **Intégrité intégrée** | Non (Nécessite HMAC externe) | Oui (Tag d'authentification) | GCM est un mode **AEAD** (Authenticated Encryption with Associated Data). Il garantit le secret et l'absence de modification des données en une seule opération. |
| **Sécurité aux attaques** | Vulnérable aux *Padding Oracle Attacks* | Immunisé contre les attaques sur le padding | CBC nécessite du padding, ce qui a causé d'immenses failles historiques (ex: POODLE). GCM fonctionne en mode compteur (CTR) et n'a pas besoin de padding. |
| **Performance** | Séquentiel (Non parallélisable) | Parallélisable | GCM est extrêmement rapide et peut être accéléré matériellement sur les processeurs modernes (instructions AES-NI). |

---

### 2.3. Anti-Replay : Nonces Temporaires vs Nonces Persistants
Pour se prémunir contre les attaques par rejeu, nous combinons un identifiant de message unique (**Nonce**) et un **Timestamp** dans chaque payload chiffré.

*   **La problématique de l'espace mémoire** : Si le serveur devait stocker tous les nonces reçus depuis le démarrage pour vérifier qu'ils ne sont pas réutilisés, la mémoire du serveur finirait par saturer ($O(N)$), créant un vecteur d'attaque par déni de service (DoS).
*   **La Solution combinée** : Le serveur rejette immédiatement tout message dont le timestamp est supérieur à 60 secondes. Grâce à cette fenêtre de validité glissante (sliding window), le serveur a uniquement besoin de stocker en mémoire les nonces reçus au cours des 60 dernières secondes. Les nonces plus anciens sont invalidés d'office par la contrainte temporelle. L'empreinte mémoire reste stable ($O(1)$ à taux de requêtes constant).

---

## 3. Justification de la Gestion d'Identité et de Session

```
             Flux d'Authentification Administrateur Challenge-Response
             
  Client JavaFX                                                    Serveur TCP
      |                                                                 |
      | 1. Demande de connexion (admin@chrionline.ma)                   |
      |---------------------------------------------------------------->|
      |                                                                 |
      | 2. Génération et envoi d'un Défi Aléatoire (Challenge UUID)     |
      |<----------------------------------------------------------------|
      |                                                                 |
      | 3. Signature du Défi avec la clé privée (admin_private_key.pem) |
      |    Signature = RSA_Sign(Challenge, PrivateKey)                  |
      |                                                                 |
      | 4. Envoi de la Signature mathématique                           |
      |---------------------------------------------------------------->|
      |                                                                 |
      |                                   5. Récupération de la clé     |
      |                                      publique depuis la DB      |
      |                                   6. Vérification mathématique  |
      |                                      de la Signature            |
      |                                                                 |
      | 5. Authentification accordée & Session créée                     |
      |<----------------------------------------------------------------|
```

### 3.1. Clés RSA vs Mots de passe pour l'Administration
Pour l'accès le plus critique (le compte Administrateur), nous avons aboli l'usage des mots de passe traditionnels au profit d'un protocole de **Challenge-Response par signature de clés**.
*   **Justification du Choix** : Les comptes d'administration sont sensibles au phishing, aux attaques par force brute et aux fuites d'identifiants (Credential Stuffing). 
    En utilisant le principe du défi-réponse RSA :
    *   Aucun mot de passe admin ne transite sur le réseau, et aucun mot de passe admin n'est stocké en base de données.
    *   Même si la base de données est compromise, l'attaquant ne récupère que la clé publique de l'admin, qui est inutile pour s'authentifier.
    *   L'attaquant doit posséder physiquement le fichier de clé privée (`admin_private_key.pem`) sur sa machine cliente pour se connecter, ce qui équivaut à un facteur de possession matériel fort.

---

### 3.2. Normalisation de la Base de Données (Stockage en Clair des Noms/Emails)
Initialement, le système cryptait les champs `nom` et `email` côté client avant persistance. Nous avons corrigé cette erreur architecturale majeure pour stocker ces données en clair (seuls les mots de passe sont hachés par BCrypt).

*   **Pourquoi le chiffrement persistant de ces champs était une mauvaise pratique ?**
    1.  **Destruction des capacités de recherche** : Chiffrer l'email ou le nom rend impossible les requêtes d'indexation SQL natives (`WHERE email = ?`), les recherches partielles (`LIKE`), les tris ou les jointures.
    2.  **Fausse impression de sécurité** : Si le client chiffre ces données avec une clé globale partagée ou que le serveur déchiffre les données à chaque lecture, la compromission de l'application livre immédiatement la clé de déchiffrement. Le bénéfice sécuritaire est nul par rapport au coût architectural énorme.
    3.  **La Solution Standard** : Assurer la sécurité en transit (via notre canal AES-GCM) et déléguer la sécurité au repos (Data at Rest) à des mécanismes de chiffrement natifs du moteur de base de données (Transparent Data Encryption - TDE) ou à des contrôles d'accès stricts sur le serveur SQL, préservant ainsi l'intégrité fonctionnelle de la base de données.

---

## 4. Choix de Conception Logicielle & Sécurité Thread-Safe

### 4.1. Algorithme de Hash des Mots de Passe : BCrypt vs SHA-256
Pour la persistance des mots de passe des utilisateurs standards, nous avons choisi **BCrypt** plutôt qu'une fonction de hachage rapide comme SHA-256 ou SHA-512.

*   **Justification** : Les algorithmes de la famille SHA sont conçus pour être extrêmement rapides (calculés en nanosecondes). C'est excellent pour vérifier l'intégrité de gros fichiers, mais catastrophique pour les mots de passe. Un attaquant doté d'un processeur graphique (GPU) moderne peut calculer des milliards de hashes SHA-256 par seconde, rendant le crack de mots de passe par force brute trivial.
*   **BCrypt** utilise un mécanisme d'étirement de clé (*Key Stretching*) basé sur le chiffrement Blowfish. Il intègre un **facteur de coût adaptatif** (configurable) qui force l'algorithme à s'exécuter lentement (environ 100 à 200 millisecondes par hash). Cette lenteur délibérée paralyse les tentatives de brute-force massives, tout en restant imperceptible pour un utilisateur légitime qui ne se connecte qu'une seule fois.

---

### 4.2. Atomicité des Vérifications CAPTCHA contre les Race Conditions
Pour éviter qu'un script malveillant ne contourne le système CAPTCHA en exploitant le multi-threading du serveur (attaque par *Race Condition* ou *Time-of-Check to Time-of-Use* - TOCTOU), le processus de validation de CAPTCHA doit être strictement atomique.

*   **La Vulnérabilité classique** :
    ```
    Fil A (Thread 1) -----------------> Check CAPTCHA valide ? (Oui) --------> Créer Compte
                                              | (Délai CPU)
    Fil B (Thread 2) -----------------> Check CAPTCHA valide ? (Oui) --------> Créer Compte
    ```
    Si la suppression du CAPTCHA de la mémoire vive se fait *après* la validation, un bot peut envoyer 50 requêtes simultanées avec le même code CAPTCHA. Elles liront toutes l'état "valide" avant que la première requête n'ait eu le temps de supprimer le jeton.
*   **La Justification de `computeIfPresent`** :
    En utilisant la méthode atomique `ConcurrentHashMap.computeIfPresent`, l'incrémentation des tentatives, la validation et la suppression éventuelle du jeton CAPTCHA s'exécutent en une seule opération atomique et thread-safe, verrouillée au niveau du bucket de la table de hachage. Aucun thread concurrent ne peut lire un état intermédiaire ou réutiliser un CAPTCHA en cours de consommation.

---

## 5. Synthèse de l'Architecture des Choix Sécuritaires

```
Couche Réseau          [ Sockets TCP Bruts ] -> Prévention des vecteurs HTTP web classiques
     |
Couche Sécurité        [ Chiffrement Hybride RSA/AES-256-GCM ] -> Confidentialité & Intégrité (AEAD)
     |
Couche Protocole       [ Nonces + Timestamps (Anti-Replay) ] -> Prévention de l'interception et rejeu
     |
Couche Session         [ IP Binding & Rotation de Tokens ] -> Neutralisation du vol de session
     |
Couche Application     [ BCrypt, PreparedStatements & BOLA Checks ] -> Protection OWASP & Intégrité DB
```

Ces choix architecturaux et cryptographiques garantissent que **ChriOnline** ne repose pas sur de la sécurité par l'obscurité, mais applique rigoureusement des standards de l'état de l'art pour fournir une plateforme e-commerce robuste et hautement sécurisée.
