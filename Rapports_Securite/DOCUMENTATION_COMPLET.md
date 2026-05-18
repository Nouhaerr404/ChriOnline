# Documentation Complète du Projet ChriOnline

## 1. Présentation du Projet
ChriOnline est une application client-serveur de commerce électronique développée en Java (JavaFX pour le client, Sockets TCP pour le serveur). Le projet se distingue par son intégration robuste de multiples couches de sécurité (cryptographie, protection réseau, gestion des identités) pour garantir la confidentialité, l'intégrité et la disponibilité des données.

---

## 2. Fonctionnalités Principales

### 2.1. Gestion des Utilisateurs (Authentification et Autorisation)
- **Inscription & Connexion** : Les utilisateurs peuvent créer un compte et s'y connecter de manière sécurisée.
- **Rôles** : Différenciation claire entre les clients (`CLIENT`) et les administrateurs (`ADMINISTRATEUR`).
- **Captcha** : Sécurisation du processus de connexion contre les attaques automatisées (bots) et la force brute.

### 2.2. Boutique et Panier
- **Catalogue de Produits** : Consultation des produits disponibles avec gestion dynamique des stocks.
- **Panier d'Achat** : Ajout, modification de la quantité et suppression d'articles du panier, persistance en base de données.

### 2.3. Commandes et Paiements
- **Validation de Commande** : Conversion du panier en commande ferme avec un état de suivi (En attente, Payée, Expédiée).
- **Paiement** : Simulation de paiement sécurisé via un canal TLS dédié.

---

## 3. Architecture et Flux Global

### Architecture Client-Serveur
L'application repose sur un modèle d'échange de requêtes/réponses asynchrones structurées par le package `protocol`.
- **Serveur TCP Principal** (`TCPServer.java`) : Gère les connexions entrantes sur un port défini. Il délègue le traitement à des `ClientHandler`.
- **SecureHandshake & SecureChannel** : Couche middleware qui assure l'échange sécurisé des clés avant toute communication métier.

### Flux de Connexion Sécurisée (Handshake)
Le processus de Handshake s'inspire de TLS (Transport Layer Security) mais est implémenté manuellement pour la démonstration cryptographique :
1. **Initiation** : Le client initie une connexion TCP.
2. **Requête PubKey** : Le client demande la clé publique RSA du serveur.
3. **Envoi PubKey** : Le serveur renvoie sa clé publique RSA.
4. **Génération AES** : Le client génère une clé de session symétrique (AES-256).
5. **Chiffrement AES** : Le client chiffre cette clé AES avec la clé publique RSA du serveur et l'envoie.
6. **Déchiffrement AES** : Le serveur déchiffre la clé AES avec sa clé privée RSA.
7. **Établissement du Canal** : Un `SecureChannel` est établi. Tous les messages suivants (Requêtes/Réponses) sont chiffrés en AES-GCM.

---

## 4. Mesures de Sécurité Détaillées

### 4.1. Chiffrement Symétrique (AES-256-GCM)
**Concept** : Chiffrement rapide adapté aux données volumineuses. L'algorithme GCM (Galois/Counter Mode) offre à la fois la confidentialité (le contenu est caché) et l'authenticité (le contenu n'a pas été altéré).
**Flux** : Utilisé dans `SecureChannel` pour chiffrer les objets (DTOs) sérialisés en Base64. Un nonce et un timestamp sont inclus pour empêcher le rejeu.
**Bout de code** (`SecureChannel.java`) :
```java
private byte[] encryptMessage(String plaintext) throws Exception {
    String nonce = UUID.randomUUID().toString(); // Prévention du rejeu
    long timestamp = System.currentTimeMillis(); // Validation d'expiration
    String prefixed = nonce + "|" + timestamp + "|" + plaintext;

    byte[] iv = AESKeyGenerator.generateIV(); // Vecteur d'initialisation unique
    byte[] ciphertext = AESEncryptor.encrypt(prefixed.getBytes(StandardCharsets.UTF_8), aesKey, iv);
    
    // Concaténation de l'IV et du message chiffré
    byte[] result = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, result, 0, iv.length);
    System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
    return result;
}
```

### 4.2. Chiffrement Asymétrique (RSA)
**Concept** : Utilisation d'une paire de clés mathématiquement liées (clé publique / clé privée). Ce qui est chiffré par l'une ne peut être déchiffré que par l'autre.
**Flux** : Utilisé uniquement lors du Handshake. Le RSA est lent, il ne sert donc qu'à transmettre la clé AES symétrique de manière sécurisée (Key Exchange) afin d'éviter l'interception (Attaque Man-In-The-Middle).
**Bout de code** (`SecureHandshake.java`) :
```java
// Le Serveur déchiffre la clé AES reçue du client en utilisant sa clé privée RSA
byte[] decryptedAESBytes = RSAEncryptor.decrypt(
        clientRequest.getEncryptedAESKey(),
        rsaManager.getServerPrivateKey()
);

// Reconstruction de la clé AES
String aesKeyBase64 = java.util.Base64.getEncoder().encodeToString(decryptedAESBytes);
this.negotiatedAESKey = AESKeyGenerator.deserializeKey(aesKeyBase64);
```

### 4.3. Protection contre les attaques SYN Flood
**Concept** : L'attaque SYN Flood consiste à envoyer de multiples paquets de synchronisation (SYN) sans jamais finaliser le handshake TCP, provoquant un épuisement des ressources du serveur (sockets semi-ouvertes).
**Mesure** : L'application intègre une classe `SYNFloodProtection` et un `SYNCookieManager` qui limitent le nombre de connexions inachevées par IP et nettoient les connexions périmées.
**Bout de code** (`TCPServer.java`) :
```java
Socket clientSocket = serverSocket.accept();
                
if (!synFloodProtection.allowConnection(clientSocket.getInetAddress())) {
    logger.warn("Connexion rejetée - protection SYN Flood: {}", 
            clientSocket.getInetAddress().getHostAddress());
    clientSocket.close();
    continue;
}
```

### 4.4. Sécurité des Mots de Passe (Hachage)
**Concept** : Ne jamais stocker les mots de passe en texte clair en base de données pour se prémunir d'une fuite de données.
**Mesure** : Hachage des mots de passe avec l'algorithme BCrypt. BCrypt intègre un "salage" (salt) automatique (empêchant les attaques par tables arc-en-ciel) et a un coût de calcul (cost factor) ajustable pour ralentir le brute-force.
**Flux** : Lors de la création du compte, `BCrypt.hashpw()` est appelé. Lors de la connexion, `BCrypt.checkpw()` est utilisé.
**Bout de code** (Exemple d'utilisation dans les DAOs) :
```java
// Hachage avant insertion
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

// Vérification au login
boolean isMatch = BCrypt.checkpw(plainPassword, userHashedPasswordFromDb);
```

### 4.5. TLS pour les Paiements (Certificats X.509)
**Concept** : Transport Layer Security. Contrairement au handshake manuel (RSA+AES), TLS s'appuie sur la JVM et des certificats standard (X.509) stockés dans un KeyStore pour authentifier le serveur et chiffrer la connexion de bout en bout.
**Mesure** : Le paiement s'effectue via un serveur dédié `TLSPaymentServer` qui utilise un `server.keystore`. Le client valide l'identité du serveur via un `client.truststore`.
**Flux** : Le client ouvre un `SSLSocket`, le système valide le certificat du serveur, et la transaction de paiement s'effectue sur ce tunnel sécurisé.

### 4.6. Signatures Numériques et KeyStore
**Concept** : Garantir l'authenticité (qui a créé le message) et la non-répudiation (l'auteur ne peut pas nier l'avoir créé).
**Mesure** : Les données sensibles ou reçus peuvent être signés cryptographiquement. Le composant `KeyStoreManager` gère l'accès sécurisé aux clés privées utilisées pour signer.
**Bout de code** (`DigitalSignatureService.java`) :
```java
public byte[] signData(byte[] data) throws Exception {
    // Utilisation de SHA-256 pour hasher la donnée, puis chiffrement du hash avec la clé privée RSA
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(keyStoreManager.getPrivateKey());
    signature.update(data);
    return signature.sign(); // Retourne la signature cryptographique
}

public boolean verifySignature(byte[] data, byte[] signatureBytes, java.security.PublicKey publicKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initVerify(publicKey);
    signature.update(data);
    return signature.verify(signatureBytes);
}
```

### 4.7. Chiffrement des Données Sensibles (Base de Données)
**Concept** : Defense-in-depth. Si un attaquant parvient à voler le fichier de la base de données SQL, les informations personnelles doivent rester inexploitables.
**Mesure** : Utilisation de la classe `SensitiveDataCipher` pour chiffrer certaines colonnes (téléphone, adresse) en base de données.
**Bout de code** (`SensitiveDataCipher.java`) :
```java
// Chiffrement avec une clé secrète côté serveur avant l'insertion SQL
public static String encryptData(String data) throws Exception {
    SecretKey key = getSystemSecretKey();
    byte[] iv = AESKeyGenerator.generateIV();
    byte[] encrypted = AESEncryptor.encrypt(data.getBytes(StandardCharsets.UTF_8), key, iv);
    return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
}
```

## Conclusion
Le projet ChriOnline démontre une application concrète des principes fondamentaux de sécurité informatique :
1. **Confidentialité** assurée par AES, RSA et TLS.
2. **Intégrité** garantie par GCM, SHA-256 et les signatures RSA.
3. **Disponibilité** renforcée par la protection anti-SYN Flood.
4. **Authentification** robuste via BCrypt et les certificats X.509.
