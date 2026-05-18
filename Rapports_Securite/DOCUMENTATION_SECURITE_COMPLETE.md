# 🛡️ ChriOnline - Documentation Exhaustive & Architecture de Sécurité

## 1. Présentation du Projet
**ChriOnline** est une plateforme e-commerce client-serveur développée en Java (JavaFX pour le client, Sockets TCP pour le serveur). Le projet a été conçu avec une **priorité absolue sur la sécurité informatique** (Security by Design), en implémentant des mécanismes cryptographiques avancés et des protections contre le Top 10 de l'OWASP.

### Fonctionnalités Principales :
*   **Boutique & Catalogue** : Navigation fluide dans les produits disponibles.
*   **Gestion de Panier** : Ajout, modification de quantité et suppression d'articles.
*   **Espace Client** : Inscription, connexion, gestion du profil et historique de commandes.
*   **Espace Administrateur** : Authentification sans mot de passe (clés asymétriques) et gestion globale de la plateforme.

---

## 2. Architecture de Sécurité Exhaustive

Dans le cadre du module de sécurité informatique, l'application implémente plusieurs couches de protection pour garantir la **Confidentialité**, l'**Intégrité**, la **Disponibilité** et la **Traçabilité** (CIDT).

### 2.1. Handshake Cryptographique Hybride (RSA + AES)
Avant toute transmission de données métiers, le client et le serveur négocient une clé de session symétrique de manière sécurisée (principe de *Key Exchange*).
*   **Flux (Flow)** :
    1.  Le serveur envoie sa **clé publique RSA** au client via le flux d'entrée/sortie.
    2.  Le client génère une **clé symétrique AES-256** robuste (clé de session).
    3.  Le client chiffre cette clé AES avec la clé publique RSA du serveur et la transmet sur le réseau.
    4.  Le serveur déchiffre le paquet avec sa **clé privée RSA** pour récupérer la clé AES. Les deux partis partagent désormais un secret inviolable.
*   **Code (Extrait de `SecureHandshake.java`)** :
```java
// --- Côté Serveur ---
KeySerializer.sendPublicKey(out, rsaKeys.getPublic());
byte[] encryptedAesKey = new byte[256];
in.readFully(encryptedAesKey);
SecretKey sessionKey = RSAEncryptor.decryptAESKey(encryptedAesKey, rsaKeys.getPrivate());
```

### 2.2. Confidentialité & Intégrité du Canal (AES-GCM)
Une fois la clé de session partagée, toutes les requêtes (`Request`) et réponses (`Response`) sont sérialisées, chiffrées et authentifiées.
*   **Mécanisme** : Utilisation de **AES-256 en mode GCM** (Galois/Counter Mode). Le mode GCM assure le chiffrement authentifié (AEAD). Cela garantit non seulement que les données sont secrètes, mais aussi qu'elles n'ont pas été altérées en transit (intégrité via un tag d'authentification cryptographique).
*   **Code (Extrait de `SecureChannel.java`)** :
```java
// Chiffrement du message avec vecteur d'initialisation (IV) aléatoire
byte[] iv = AESKeyGenerator.generateIV();
byte[] ciphertext = AESEncryptor.encrypt(prefixedData, aesKey, iv);
```

### 2.3. Protection contre les Attaques par Rejeu (Anti-Replay)
Un attaquant interceptant un paquet chiffré ne peut pas le renvoyer au serveur pour rejouer une action légitime (ex: vider le panier à l'infini, refaire un paiement).
*   **Mécanisme** : Le corps de chaque message avant chiffrement est préfixé par un **Nonce** (UUID unique) et un **Timestamp** (horodatage).
*   **Flux (Flow)** :
    1.  Le destinataire déchiffre le message et extrait le Nonce et le Timestamp.
    2.  **Vérification temporelle** : Si le message est plus vieux que 60 secondes, il est rejeté (expiré).
    3.  **Vérification du Nonce** : Si le `Nonce` a déjà été observé dans le cache mémoire récent, le message est rejeté (rejeu intercepté).
*   **Code (Extrait de `SecureChannel.java`)** :
```java
if (System.currentTimeMillis() - timestamp > MESSAGE_VALIDITY_MS)
    throw new SecurityException("Message expiré (Tentative de rejeu interceptée)");
    
if (usedNonces.putIfAbsent(nonce, timestamp) != null)
    throw new SecurityException("Nonce déjà utilisé (Attaque par rejeu bloquée)");
```

### 2.4. Gestion Sécurisée des Sessions (Anti-Hijacking & Rotation)
La gestion de session ne fait jamais confiance aux données fournies par le client pour établir l'identité de manière persistante.
*   **Liaison IP (IP Binding)** : Lors de la connexion, le token de session est strictement lié à l'adresse IP du client. Si un attaquant vole le token (Session Hijacking) et tente de l'utiliser depuis une autre IP, le serveur détecte l'anomalie et détruit la session immédiatement.
*   **Rotation des Tokens (Token Rotation)** : Les tokens de session sont régénérés et remplacés côté serveur toutes les 5 minutes de manière transparente. Cela réduit massivement la fenêtre d'opportunité d'un attaquant en cas de fuite de token.
*   **Code (Extrait de `SessionManager.java`)** :
```java
// Protection contre le Session Hijacking (Vérification de l'IP)
if (currentIP != null && !currentIP.equals(details.clientIP)) {
    activeSessions.remove(currentToken);
    return new SessionResult(false, null, "Alerte Sécurité : IP changée, session invalidée.");
}

// Rotation périodique des Tokens (ex: toutes les 5 minutes)
if (currentTime - details.tokenCreationTime > REGENERATION_INTERVAL_MS) {
    String newToken = UUID.randomUUID().toString();
    activeSessions.put(newToken, details);
    activeSessions.remove(currentToken);
}
```

### 2.5. Authentification Administrateur (Challenge-Response RSA)
L'accès administrateur est la cible de plus haute valeur. Plutôt que de s'appuyer sur un simple mot de passe vulnérable, l'application impose une preuve cryptographique asymétrique.
*   **Flux (Flow)** :
    1.  L'admin tente de se connecter. Le serveur génère un **défi aléatoire** (Challenge UUID) et le lui transmet.
    2.  Le client JavaFX utilise le fichier de la clé privée locale (`admin_private_key.pem`) de l'administrateur pour **signer numériquement** ce défi.
    3.  La signature est renvoyée au serveur.
    4.  Le serveur récupère la clé publique RSA de l'admin depuis la base de données et vérifie la validité mathématique de la signature. Si l'attaquant ne possède pas la clé privée physique, l'accès est mathématiquement impossible.
*   **Code (Extrait de `AdminAuthService.java` et Client)** :
```java
// Serveur vérifie la signature du challenge
boolean isSignatureValid = RSAVerifier.verifySignature(challenge, signatureBase64, publicKey);
if (!isSignatureValid) {
    return new Response(false, "Signature cryptographique invalide. Accès refusé.");
}
```

### 2.6. Contrôle d'Accès BOLA/IDOR (Broken Object Level Authorization)
Chaque requête effectuant une action sur les données d'un utilisateur (lire l'historique, modifier le panier, payer) vérifie rigoureusement l'appartenance de la ressource.
*   **Mécanisme** : Le serveur refuse de faire confiance à l'ID utilisateur passé dans la requête du client. Au lieu de cela, il extrait l'ID **réel** de l'utilisateur à partir de son token de session validé, et le compare avec l'ID ciblé.
*   **Robuste Face à la Désérialisation** : Utilisation stricte de la méthode `.equals()` pour la comparaison d'objets `Integer` afin de contourner les vulnérabilités de comparaison de références mémoires (`!=`) introduites lors de la désérialisation Java.
*   **Code (Extrait de `ClientHandler.java`)** :
```java
Integer sessionUserId = SessionManager.getUserId(request.getToken());

// Vérification de l'identité réelle (BOLA check)
if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
    return new Response(false, "Accès non autorisé à cet historique/panier.");
}
```

### 2.7. Protection Contre le Brute Force
*   **Verrouillage de Compte (Account Lockout)** : Au bout de plusieurs tentatives de mot de passe échouées, le compte utilisateur est temporairement verrouillé pour décourager les attaques par dictionnaire ou force brute. Les tentatives sont journalisées en base de données.
*   **Rate Limiting** : Des filtres de limitation de taux de requêtes (Token Bucket) sont implémentés pour éviter l'épuisement des ressources du serveur (DDoS applicatif).

### 2.8. Sécurisation du Système CAPTCHA Anti-Bot
Le système CAPTCHA empêche les scripts automatisés de saturer la base de données ou de réaliser du credential stuffing.
*   **Génération Côté Serveur** : L'ID de session CAPTCHA est généré par le serveur. Le client ne peut pas falsifier son propre ID.
*   **Limitation Stricte Thread-Safe** : Pour empêcher un bot de spammer des requêtes de vérification sur une seule session CAPTCHA facile, le serveur limite à **3 tentatives maximum** de résolution. L'opération est rendue atomique et sûre face aux accès concurrents (Race Conditions) grâce à `ConcurrentHashMap.computeIfPresent`.
*   **Code (Extrait de `CaptchaService.java`)** :
```java
// Vérification atomique avec limite d'essais
activeCaptchas.computeIfPresent(sessionId, (key, session) -> {
    session.incrementAttempts();
    if (session.getAttempts() >= 3) return null; // Détruit le CAPTCHA si abus
    return session;
});
```

### 2.9. Stockage Sécurisé des Mots de Passe
En cas de compromission de la base de données, les mots de passe des utilisateurs sont protégés.
*   **Mécanisme (BCrypt)** : Utilisation de **BCrypt**, un algorithme de hachage unidirectionnel lent. Un "Salt" (sel) unique est automatiquement généré et incorporé pour chaque mot de passe, rendant les attaques par tables arc-en-ciel (Rainbow Tables) obsolètes. Le coût est défini à 12, augmentant significativement le temps de calcul pour décourager le crack hors ligne.
*   **Code (Extrait de `UserService.java`)** :
```java
// Hachage avec sel automatique et coût de 12
String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12)); 
```

### 2.10. Validation des Entrées et Prévention des Injections SQL
*   **Mécanisme (JDBC PreparedStatement)** : Toutes les interactions avec la base de données (MySQL) utilisent l'API `PreparedStatement`. Cela sépare strictement la logique SQL des données utilisateur, rendant les **Injections SQL** structurellement impossibles.

---
*Ce document résume l'architecture de sécurité avancée de la solution ChriOnline, démontrant l'application rigoureuse des principes de cybersécurité modernes.*
