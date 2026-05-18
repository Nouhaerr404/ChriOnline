# DOCUMENTATION TECHNIQUE : IMPLÉMENTATION DE LA SÉCURITÉ
## Projet ChriOnline — Sécurité de Bout en Bout

> [!NOTE]
> Cette documentation présente l'implémentation concrète, détaillée et illustrée par le code des différents mécanismes de sécurité intégrés au sein de la solution e-commerce **ChriOnline** (Architecture Client/Serveur JavaFX & Sockets).

---

## Table des Matières
1. [Introduction & Philosophie "Security-by-Design"](#1-introduction--philosophie-security-by-design)
2. [Cryptographie & Sécurisation des Canaux Réseau](#2-cryptographie--s%C3%A9curisation-des-canaux-r%C3%A9seau)
   - [Handshake Hybride (RSA + AES)](#handshake-hybride-rsa--aes)
   - [Canal Réseau Sécurisé (AES-256-GCM & Anti-Rejeu)](#canal-r%C3%A9seau-s%C3%A9curis%C3%A9-aes-256-gcm--anti-rejeu)
3. [Sécurité Réseau Sockets & Couche de Transport](#3-s%C3%A9curit%C3%A9-r%C3%A9seau-sockets--couche-de-transport)
   - [SYN Flood & Protection DOS](#syn-flood--protection-dos)
   - [SYN Cookies Crytographiques](#syn-cookies-crytographiques)
   - [Serveur de Paiement Sécurisé SSL/TLS](#serveur-de-paiement-s%C3%A9curis%C3%A9-ssltls)
4. [Gestion du KeyStore Java & Signatures Numériques](#4-gestion-du-keystore-java--signatures-num%C3%A9riques)
   - [Chargement et Gestion du KeyStore (PKCS12)](#chargement-et-gestion-du-keystore-pkcs12)
   - [Signatures Numériques (SHA256withRSA)](#signatures-num%C3%A9riques-sha256withrsa)
5. [Authentification, Identités & Contrôles d'Accès](#5-authentification-identit%C3%A9s--contr%C3%B4les-dacc%C3%A8s)
   - [Authentification Admin par Challenge-Response (RSA & PEM)](#authentification-admin-par-challenge-response-rsa--pem)
   - [Sécurisation Admin : Enforcement Réseau Interne (IP)](#s%C3%A9curisation-admin--enforcement-r%C3%A9seau-interne-ip)
   - [Double Authentification (MFA/2FA via OTP Email)](#double-authentification-mfa2fa-via-otp-email)
   - [Hachage BCrypt & Processus de Migration Transparente](#hachage-bcrypt--processus-de-migration-transparente)
   - [Politique de Blocage Anti-Brute Force (Lockout Palier)](#politique-de-blocage-anti-brute-force-lockout-palier)
   - [Protection Anti-Bot (CAPTCHA de Niveau Production)](#protection-anti-bot-captcha-de-niveau-production)
6. [Sécurité Applicative, Autorisations Métier & Données](#6-s%C3%A9curit%C3%A9-applicative-autorisations-m%C3%A9tier--donn%C3%A9es)
   - [Broken Object Level Authorization (BOLA/IDOR)](#broken-object-level-authorization-bolaidor)
   - [Prévention du Rejeu Métier (Cooldown Paiements)](#pr%C3%A9vention-du-rejeu-m%C3%A9tier-cooldown-paiements)
   - [Protection Absolue contre les Injections SQL](#protection-absolue-contre-les-injections-sql)
   - [Chiffrement des Données Sensibles At-Rest (Base de Données)](#chiffrement-des-donn%C3%A9es-sensibles-at-rest-base-de-donn%C3%A9es)

---

## 1. Introduction & Philosophie "Security-by-Design"

Le projet **ChriOnline** a été entièrement conçu selon la philosophie du **Security-by-Design**. Face aux menaces pesant sur les architectures distribuées (interception de données, usurpation de session, injections, attaques par déni de service, etc.), la sécurité n'a pas été ajoutée comme une surcouche de fin de projet, mais intégrée dès le départ dans chaque couche logique (Réseau, Transport, Session, Authentification, Autorisation et Données).

Cette approche offre une protection robuste respectant le principe de la **Défense en Profondeur**, où chaque barrière de sécurité protège le système si une autre venait à faillir.

---

## 2. Cryptographie & Sécurisation des Canaux Réseau

### Handshake Hybride (RSA + AES)
Afin d'éviter la distribution statique de clés symétriques (faille majeure de sécurité), ChriOnline met en œuvre un protocole de **Handshake Hybride inspiré de TLS/HTTPS** :
1. Le client contacte le serveur en envoyant un nonce de session unique.
2. Le serveur répond en envoyant sa clé publique RSA générée de manière éphémère ou extraite.
3. Le client génère une clé symétrique temporaire **AES-256** hautement aléatoire via un générateur cryptographique fort.
4. Le client chiffre cette clé AES avec la clé publique RSA du serveur et la transmet.
5. Le serveur déchiffre la clé AES en utilisant sa clé privée RSA, l'établit comme clé de session, et confirme le handshake.

*   **Classes impliquées** : [SecureHandshake.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/SecureHandshake.java), [RSAKeyManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/RSAKeyManager.java), [AESKeyGenerator.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/AESKeyGenerator.java).

#### Extrait de code de l'établissement côté Client ([SecureHandshake.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/SecureHandshake.java#L168-L212)) :
```java
public HandshakeRequest sendEncryptedAESKey(HandshakeResponse serverResponse, String clientNonce) throws Exception {
    // 1. Valider la réponse du serveur (nonce & timestamp)
    if (!serverResponse.isValid(NONCE_VALIDITY_MS)) {
        throw new SecurityException("Réponse serveur expirée ou invalide");
    }

    try {
        // 2. Décoder la clé publique RSA du serveur depuis Base64
        byte[] decodedKey = Base64.getDecoder().decode(serverResponse.getPublicKeyBase64());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey serverPublicKey = factory.generatePublic(spec);

        // 3. Générer une clé symétrique AES-256 cryptographiquement sécurisée
        this.negotiatedAESKey = AESKeyGenerator.generateKey();
        byte[] aesKeyBytes = this.negotiatedAESKey.getEncoded();

        // 4. Chiffrer la clé AES avec la clé publique RSA du serveur
        byte[] encryptedAES = RSAEncryptor.encrypt(aesKeyBytes, serverPublicKey);

        // 5. Encapsuler dans la requête de handshake finale
        HandshakeRequest request = new HandshakeRequest(clientNonce, encryptedAES);
        this.handshakeComplete = true;

        return request;
    } catch (Exception e) {
        throw new SecurityException("Impossible de compléter le handshake hybride", e);
    }
}
```

---

### Canal Réseau Sécurisé (AES-256-GCM & Anti-Rejeu)
Une fois le handshake terminé, toutes les communications ultérieures abandonnent le protocole RSA (trop lourd) et transitent exclusivement par un **canal sécurisé symétrique** chiffré en **AES-256-GCM**.
- **Confidentialité & Intégrité** : Le mode GCM (Galois/Counter Mode) est un algorithme AEAD (Authenticated Encryption with Associated Data). Il assure non seulement le chiffrement, mais génère également un tag d'authentification cryptographique de 128 bits empêchant toute altération en transit par un attaquant MITM.
- **Protection Anti-Rejeu** : Chaque message réseau individuel est encapsulé avec un nonce unique (UUID) et un timestamp d'émission. Le récepteur déchiffre, valide le tag GCM, puis vérifie que le timestamp est inférieur à 1 minute et que le nonce n'a jamais été vu (stockage temporaire dans une table de hachage).

*   **Classes impliquées** : [SecureChannel.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/SecureChannel.java), [AESEncryptor.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/AESEncryptor.java).

#### Implémentation du chiffrement & injection anti-rejeu ([SecureChannel.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/SecureChannel.java#L68-L107)) :
```java
private byte[] encryptMessage(String plaintext) throws Exception {
    String nonce = UUID.randomUUID().toString();
    long timestamp = System.currentTimeMillis();
    // Injection du Nonce et du Timestamp dans la structure interne du message
    String prefixed = nonce + "|" + timestamp + "|" + plaintext;

    byte[] iv = AESKeyGenerator.generateIV(); // IV unique de 12 octets
    byte[] ciphertext = AESEncryptor.encrypt(prefixed.getBytes(StandardCharsets.UTF_8), aesKey, iv);

    // Concaténation de l'IV (en clair) et du ciphertext crypté
    byte[] result = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, result, 0, iv.length);
    System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
    return result;
}

private String decryptMessage(byte[] encrypted) throws Exception {
    if (encrypted.length < 12) throw new SecurityException("Message trop court");
    
    byte[] iv = new byte[12];
    System.arraycopy(encrypted, 0, iv, 0, 12);
    byte[] ciphertext = new byte[encrypted.length - 12];
    System.arraycopy(encrypted, 12, ciphertext, 0, ciphertext.length);

    // Déchiffrement AES-GCM (valide implicitement l'intégrité via le tag)
    byte[] decrypted = AESEncryptor.decrypt(ciphertext, aesKey, iv);
    String plaintext = new String(decrypted, StandardCharsets.UTF_8);

    // Parsing et extraction des contrôles anti-rejeu
    String[] parts = plaintext.split("\\|", 3);
    if (parts.length < 3) throw new SecurityException("Format de message invalide");

    String nonce = parts[0];
    long timestamp = Long.parseLong(parts[1]);
    String data = parts[2];

    // Vérification de la fraîcheur temporelle (fenêtre active de 60 secondes)
    if (System.currentTimeMillis() - timestamp > MESSAGE_VALIDITY_MS)
        throw new SecurityException("Message expiré (Tentative de rejeu temporel)");
        
    // Vérification de l'unicité du nonce
    if (usedNonces.containsKey(nonce))
        throw new SecurityException("Nonce déjà consommé (Rejeu détecté)");
    
    usedNonces.put(nonce, System.currentTimeMillis());
    return data;
}
```

---

## 3. Sécurité Réseau Sockets & Couche de Transport

### SYN Flood & Protection DOS
Pour empêcher les attaques de type SYN Flood (épuisement des ressources par demi-connexions TCP ouvertes à l'infini), le serveur implémente un module de surveillance des connexions.
- Il applique une limite stricte de connexions simultanées par adresse IP (`MAX_CONNECTIONS_PER_IP = 100`).
- Il conserve un registre des connexions "en attente" d'authentification ou d'établissement de handshake.
- Un thread de nettoyage s'assure d'éliminer de la mémoire toutes les sessions stagnantes au-delà de 10 secondes.

*   **Classe impliquée** : [SYNFloodProtection.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/security/SYNFloodProtection.java).

#### Extrait de code de filtrage des nouvelles sockets ([SYNFloodProtection.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/security/SYNFloodProtection.java#L22-L44)) :
```java
public boolean allowConnection(InetAddress clientAddress) {
    String ip = clientAddress.getHostAddress();
    
    // 1. Limiter le débit par IP
    if (isIPRateLimited(ip)) {
        logger.warn("IP {} bloquée - limite de connexions dépassée", ip);
        return false;
    }
    
    // 2. Limiter la capacité totale de bufferisation du serveur
    if (isMaxPendingReached()) {
        logger.warn("Buffer de connexions pendantes plein. Nouvelle connexion refusée.");
        return false;
    }
    
    recordPendingConnection(ip);
    return true;
}
```

---

### SYN Cookies Crytographiques
Pour contrer le spoofing d'adresses IP durant les attaques DDoS, le serveur dispose d'un manager de **SYN Cookies**. Ce mécanisme permet au serveur d'encoder cryptographiquement l'empreinte de la connexion client (`IP + Port + Secret serveur + Salt aléatoire`) et de la valider au moment de la confirmation finale de la socket.

*   **Classe impliquée** : [SYNCookieManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/security/SYNCookieManager.java).

#### Algorithme de génération cryptographique ([SYNCookieManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/security/SYNCookieManager.java#L76-L95)) :
```java
private String generateHash(String data) {
    try {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        String input = data + SECRET_KEY + random.nextInt();
        byte[] hash = digest.digest(input.getBytes("UTF-8"));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    } catch (Exception e) {
        return String.valueOf(random.nextLong());
    }
}
```

---

### Serveur de Paiement Sécurisé SSL/TLS
Les transactions de paiement nécessitent une sécurité renforcée. ChriOnline implémente un serveur de paiement dédié utilisant des **Sockets SSL/TLS strictes** :
- Le serveur de paiement écoute sur le port `9999` (défini dans `.env`).
- Il n'accepte que des connexions utilisant le protocole TLS v1.3.
- Il charge un KeyStore Java (`server-keystore.jks`) contenant son certificat signé et son couple de clés privée/publique, ainsi qu'un TrustStore (`client-truststore.jks`) pour valider mutuellement l'identité du client effectuant le paiement.

*   **Classe impliquée** : [TLSPaymentServer.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TLSPaymentServer.java).

#### Initialisation du contexte TLS sécurisé ([TLSPaymentServer.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TLSPaymentServer.java#L47-L80)) :
```java
public void start() throws Exception {
    // 1. Configurer le contexte SSL avec le protocole TLS strict
    SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
    
    // 2. Initialiser le KeyManager avec server-keystore.jks
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    KeyStore ks = KeyStore.getInstance("JKS");
    try (FileInputStream fis = new FileInputStream(keystorePath)) {
        ks.load(fis, keystorePassword.toCharArray());
    }
    kmf.init(ks, keystorePassword.toCharArray());
    
    // 3. Initialiser le TrustManager avec client-truststore.jks (Authentification Mutuelle)
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    KeyStore ts = KeyStore.getInstance("JKS");
    try (FileInputStream fis = new FileInputStream(truststorePath)) {
        ts.load(fis, truststorePassword.toCharArray());
    }
    tmf.init(ts);
    
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
    SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
    
    this.serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
    this.serverSocket.setNeedClientAuth(true); // Exiger le certificat client
    logger.info("Serveur de paiement TLS démarré sur le port " + port);
}
```

---

## 4. Gestion du KeyStore Java & Signatures Numériques

### Chargement et Gestion du KeyStore (PKCS12)
Pour garantir la non-répudiation et l'intégrité des reçus de transaction et des validations système, le serveur charge un fichier KeyStore standard PKCS12 nommé `monkeystore.p12` à partir de son classpath à l'aide de la JCA (Java Cryptography Architecture).
- Le keystore stocke de manière hautement sécurisée (protégée par mot de passe) la clé privée du système permettant de générer des signatures électroniques indéniables.

*   **Classe impliquée** : [KeyStoreManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/KeyStoreManager.java).

#### API de chargement et d'extraction ([KeyStoreManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/KeyStoreManager.java#L24-L48)) :
```java
public KeyStoreManager(String keystorePath, String password) throws Exception {
    this.keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(keystorePath)) {
        if (is == null) {
            throw new Exception("Keystore introuvable dans le classpath: " + keystorePath);
        }
        keyStore.load(is, password.toCharArray());
    }
}

public PrivateKey getPrivateKey(String alias, String keyPassword) throws Exception {
    KeyStore.Entry entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(keyPassword.toCharArray()));
    if (entry instanceof KeyStore.PrivateKeyEntry) {
        return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }
    return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
}
```

---

### Signatures Numériques (SHA256withRSA)
La classe [DigitalSignatureService](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/DigitalSignatureService.java) fournit les méthodes d'encapsulation de la signature. Elle exploite l'algorithme fort `SHA256withRSA` pour signer et vérifier des payloads.

#### Code de Signature et Vérification ([DigitalSignatureService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/DigitalSignatureService.java#L15-L44)) :
```java
public byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(privateKey);
    signature.update(data);
    return signature.sign();
}

public boolean verifySignature(byte[] data, byte[] signatureBytes, PublicKey publicKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initVerify(publicKey);
    signature.update(data);
    return signature.verify(signatureBytes);
}
```

---

## 5. Authentification, Identités & Contrôles d'Accès

### Authentification Admin par Challenge-Response (RSA & PEM)
Les administrateurs ne possèdent pas de mot de passe en base de données. Pour éviter le vol d'identifiants et éliminer le risque d'attaques par dictionnaire sur l'accès le plus critique du système, ChriOnline implémente un système **d'authentification asymétrique par Challenge-Response** :
1. L'administrateur saisit son email et sélectionne sa **clé privée locale au format PEM** (`.pem`).
2. Le client demande un challenge au serveur via l'action `GENERATE_CHALLENGE_ADMIN`.
3. Le serveur génère un challenge aléatoire cryptographique (UUID) et l'associe à l'email pour une durée de 30 secondes maximum dans une table `ConcurrentHashMap`.
4. Le client signe ce challenge avec sa clé privée locale en utilisant l'algorithme RSA.
5. Le client transmet l'email, le challenge et la signature Base64 au serveur.
6. Le serveur récupère la clé publique de l'administrateur stockée de façon sécurisée dans sa base de données, décode la signature et valide cryptographiquement que la signature correspond bien au challenge.
7. Si la signature est correcte et le challenge valide (non expiré, non réutilisé), le serveur autorise la connexion admin et génère le token de session.

*   **Classes impliquées** : [AdminAuthService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/AdminAuthService.java), [AdminAuthClient.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/client/security/AdminAuthClient.java), [AdminLoginView.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/client/views/AdminLoginView.java), [UserService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/UserService.java#L181-L228).

#### Validation côté serveur ([UserService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/UserService.java#L181-L210)) :
```java
public static Response loginAdminChallenge(Object data, String clientIP) {
    try {
        Object[] payload = (Object[]) data;
        String email = (String) payload[0];
        String challenge = (String) payload[1];
        String signatureBase64 = (String) payload[2];

        // 1. Récupérer et consommer immédiatement le challenge pour éviter les attaques par rejeu
        String storedChallenge = adminChallenges.remove(email);
        if (storedChallenge == null || !storedChallenge.equals(challenge)) {
            return new Response(false, "Challenge invalide ou expiré.");
        }

        Utilisateur u = dao.trouverAdminParEmail(email);
        if (u == null) return new Response(false, "Administrateur introuvable.");

        // 2. Extraire la clé publique RSA de l'administrateur depuis la base de données
        String publicKeyBase64 = dao.getPublicKeyByEmail(email);
        if (publicKeyBase64 == null || publicKeyBase64.isEmpty()) {
            return new Response(false, "Clé publique manquante.");
        }

        // 3. Valider cryptographiquement la signature RSA
        java.security.PublicKey publicKey = KeySerializer.deserializePublicKey(publicKeyBase64);
        boolean isSignatureValid = RSAVerifier.verifyBase64(challenge, signatureBase64, publicKey);

        if (!isSignatureValid) {
            logger.warn("Échec signature RSA pour admin : " + email);
            return new Response(false, "Signature invalide.");
        }
        ...
```

#### Parsing de la clé privée PEM locale côté client ([AdminLoginView.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/client/views/AdminLoginView.java#L165-L174)) :
```java
private PrivateKey loadPrivateKey(File file) throws Exception {
    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
    // Nettoyage des entêtes PEM PKCS8
    content = content.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", ""); // Supprimer les retours à la ligne
    return KeySerializer.deserializePrivateKey(content);
}
```

---

### Sécurisation Admin : Enforcement Réseau Interne (IP)
En complément du protocole Challenge-Response, le serveur applique une **règle de réseau strict (IP Enforcement)** :
- Un administrateur n'a le droit de se connecter ou de valider son authentification que s'il émet sa requête depuis une **adresse IP du réseau privé/interne** (ex: loopback, `192.168.x.x`, `10.x.x.x`, `172.16.x.x-172.31.x.x`).
- Toute tentative d'authentification admin depuis une adresse IP externe/publique est immédiatement bloquée.

#### Filtrage d'IP Admin ([UserService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/UserService.java#L488-L512)) :
```java
private static Response verifierAccesIPAdmin(Utilisateur u, String clientIP, String identifier) {
    if ("ADMINISTRATEUR".equals(u.getTypeCompte()) && !isInternalIP(clientIP)) {
        logger.warn("Accès bloqué - Admin sur IP externe : {} | Identifiant : {}", clientIP, identifier);
        return new Response(false, "Accès administrateur refusé : vous devez être connecté au réseau interne.");
    }
    return null;
}

private static boolean isInternalIP(String ip) {
    if (ip == null || ip.isBlank()) return false;
    return ip.startsWith("192.168.")
            || ip.startsWith("10.")
            || ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")
            || ip.equals("127.0.0.1")
            || ip.equals("0:0:0:0:0:0:0:1")
            || ip.equals("::1");
}
```

---

### Double Authentification (MFA/2FA via OTP Email)
Pour les comptes utilisateurs "Clients" ayant activé la double authentification :
1. Une fois le mot de passe validé, le serveur génère un code OTP aléatoire à 6 chiffres.
2. Ce code est stocké en mémoire dans la classe [OtpStore](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/OtpStore.java) associée à un timestamp d'expiration (valide 5 minutes).
3. Le code est envoyé par mail au format HTML de manière asynchrone pour ne pas bloquer le thread réseau principal.
4. L'application client bascule sur la vue de validation OTP. Si l'OTP saisi correspond et n'a pas expiré, la session est ouverte. L'OTP est immédiatement consommé (supprimé de la mémoire).

*   **Classes impliquées** : [OtpStore.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/OtpStore.java), [UserService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/UserService.java#L123-L137), `EmailService.java`.

#### Logique de stockage de l'OTP en mémoire ([OtpStore.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/OtpStore.java#L7-L39)) :
```java
public class OtpStore {
    private static final long OTP_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private record OtpEntry(String code, long expiresAt) {}
    private static final Map<Integer, OtpEntry> store = new ConcurrentHashMap<>();

    public static String generateAndStore(int userId) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put(userId, new OtpEntry(code, System.currentTimeMillis() + OTP_TTL_MS));
        return code;
    }

    public static boolean validate(int userId, String code) {
        OtpEntry entry = store.get(userId);
        if (entry == null) return false;
        
        if (System.currentTimeMillis() > entry.expiresAt()) {
            store.remove(userId);
            return false;
        }
        
        boolean valid = entry.code().equals(code);
        if (valid) store.remove(userId); // Empêche la réutilisation du code validé
        return valid;
    }
}
```

---

### Hachage BCrypt & Processus de Migration Transparente
Afin de moderniser la persistance et d'éliminer les algorithmes de hachage obsolètes (SHA-256 statique sans sel, vulnérable aux attaques par tables de correspondance), le système intègre la bibliothèque standard **BCrypt** avec un facteur de coût élevé (`12`).

Le système implémente une **migration transparente (on-the-fly) des anciens algorithmes** :
1. Si un utilisateur se connecte, le système vérifie le format du mot de passe stocké en BDD.
2. Si le hash commence par `$2a$` (ou `$2b$`, `$2y$`), il valide directement via BCrypt.
3. Si le hash est au format SHA-256 brut (legacy), le mot de passe fourni par l'utilisateur est haché avec SHA-256 pour vérification. S'il correspond, le système calcule immédiatement le nouveau hash **BCrypt** et met à jour automatiquement la base de données.
4. Ainsi, les anciens mots de passe sont mis à niveau sans aucune perturbation pour l'utilisateur ni besoin de réinitialisation massive.

*   **Classe impliquée** : [UtilisateurDAO.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java).

#### Migration dynamique ([UtilisateurDAO.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java#L201-L248)) :
```java
Utilisateur u = dao.trouverParEmailPassword(email, password);
// ...
String hashStocke = rs.getString("password");
if (!verifierMotDePasse(password, hashStocke)) {
    return null;
}
// Si le hash stocké est de l'ancien format SHA-256 (ne commence pas par $2)
if (!hashStocke.startsWith("$2")) {
    // Calculer le nouveau hash fort avec BCrypt et le sauvegarder
    mettreAJourHashMotDePasse(id, hasherMotDePasse(password));
    logger.info("Migration SHA-256 -> BCrypt effectuée pour {}", email);
}
```

---

### Politique de Blocage Anti-Brute Force (Lockout Palier)
Pour interdire les attaques par dictionnaire ou brute force, ChriOnline définit une politique de verrouillage de compte dynamique basée sur des **paliers temporels** :
- Le nombre maximum de tentatives successives autorisées est fixé à `3`.
- Après 3 échecs, le compte est bloqué temporairement.
- La durée du blocage augmente de manière incrémentale selon la formule :
  $$\text{Durée} = \text{Niveau de Blocage} \times 5 \text{ minutes}$$
- Le niveau de blocage est stocké de manière thread-safe dans un dictionnaire `ConcurrentHashMap`. Un premier verrouillage dure 5 minutes, le second 10 minutes, et ainsi de suite.

*   **Classe impliquée** : [UtilisateurDAO.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java#L72-L150).

---

### Protection Anti-Bot (CAPTCHA de Niveau Production)
Afin d'empêcher les scripts d'inscription automatique (bots de spam) et le bourrage d'identifiants (Credential Stuffing), la connexion et l'inscription exigent la validation d'un CAPTCHA visuel généré dynamiquement par le serveur :
1. **Rendu Visuel Robuste** : Le serveur utilise l'API standard `AWT/Java2D` pour générer une image PNG de 220x65 pixels. Il applique un fond sombre premium, introduit un bruit de fond (250 points perturbateurs), dessine 4 courbes d'interférence colorées et trace chaque lettre du code (d'une longueur de 6 à 7 caractères, en excluant les caractères ambigus comme `0, O, I, 1`) avec une translation, un cisaillement local (shear) et une rotation aléatoire comprise entre -25° et +25°.
2. **Vérification Atomique** : Le code généré est associé à un token de session et stocké en mémoire. La validation supprime immédiatement le CAPTCHA si la saisie est correcte. En cas d'erreur, le compteur d'essais est incrémenté. Au bout de **3 tentatives invalides**, le CAPTCHA est détruit, forçant le renouvellement de l'image.
3. **Protection Globale par Sliding Window (IP Rate Limiting)** : Pour éviter les attaques d'épuisement de ressources (DoS) consistant à demander des milliers d'images de CAPTCHA par seconde, le service intègre un limiteur de débit glissant par IP, restreignant les demandes à **maximum 5 CAPTCHAs par 10 secondes** par adresse IP.

*   **Classe impliquée** : [CaptchaService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/CaptchaService.java).

#### Implémentation du Sliding Window Rate Limiter ([CaptchaService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/CaptchaService.java#L102-L121)) :
```java
public static boolean allowRequest(String clientIP) {
    long now = System.currentTimeMillis();
    long windowMs = 10000; // 10 secondes
    int maxRequests = 5;

    List<Long> timestamps = rateLimits.computeIfAbsent(clientIP, k -> new CopyOnWriteArrayList<>());
    
    // Purger les timestamps hors-fenêtre (plus vieux de 10 secondes)
    timestamps.removeIf(t -> (now - t) > windowMs);

    if (timestamps.size() < maxRequests) {
        timestamps.add(now);
        return true;
    }
    logger.warn("Rate limit CAPTCHA activé pour l'IP : " + clientIP);
    return false;
}
```

---

## 6. Sécurité Applicative, Autorisations Métier & Données

### Broken Object Level Authorization (BOLA/IDOR)
Une vulnérabilité majeure des applications transactionnelles est l'**IDOR/BOLA** (Broken Object Level Authorization), où un attaquant modifie un identifiant dans les paramètres d'une requête pour accéder au panier ou aux commandes d'un autre utilisateur.

ChriOnline élimine systématiquement ce risque au niveau de son routeur d'actions principal [ClientHandler](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/network/ClientHandler.java) :
- À l'ouverture de session, le token généré est enregistré en mémoire serveur associé à l'ID de l'utilisateur (`userId`).
- Lors de la réception de toute requête métier (ex: afficher le panier, modifier le profil, payer une commande, lister l'historique), le serveur extrait l'ID utilisateur lié au token fourni dans la requête.
- Il compare strictement cet ID de session interne à l'ID de l'objet cible envoyé dans la payload de la requête. S'ils diffèrent, l'accès est immédiatement bloqué avec un message d'erreur d'autorisation.

#### Exemple de double validation IDOR sur les paniers ([ClientHandler.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/network/ClientHandler.java#L329-L336)) :
```java
case "AFFICHER_PANIER": {
    int targetClientId = Integer.parseInt(request.getData().toString());
    Integer sessionUserId = ma.ensate.server.services.SessionManager.getUserId(request.getToken());
    
    // Blocage IDOR : Empêche un client X de consulter le panier du client Y
    if (sessionUserId == null || !sessionUserId.equals(targetClientId)) {
        return new Response(false, "Accès non autorisé à ce panier.");
    }
    return servicePanier.obtenirPanierResponse(targetClientId);
}
```

---

### Prévention du Rejeu Métier (Cooldown Paiements)
Pour éviter qu'un utilisateur n'effectue accidentellement un double-paiement (clics frénétiques en cas de latence réseau) ou qu'un attaquant ne rejoue une trame réseau de paiement valide interceptée, le service de paiement intègre un **PaymentRateLimiter**.
- Il génère une clé de cooldown unique combinant l'ID client et le montant exact : `clientId_montant`.
- Il applique une politique de rejet systématique des requêtes identiques pendant une durée de **30 secondes** (cooldown).
- Cette approche garantit la navigation fluide de l'utilisateur (qui peut enchaîner des achats de montants distincts sans blocage) tout en coupant net toute tentative de double-soumission accidentelle ou malveillante.

*   **Classe impliquée** : [PaymentRateLimiter.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/PaymentRateLimiter.java).

#### Logique de cooldown ([PaymentRateLimiter.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/PaymentRateLimiter.java#L19-L37)) :
```java
public static boolean isReplayAttack(String clientId, double montant) {
    String key = clientId + "_" + montant;
    long currentTime = System.currentTimeMillis();

    // Insertion atomique. Renvoie le timestamp précédent s'il existe déjà
    Long lastAttemptTime = recentPayments.putIfAbsent(key, currentTime);

    if (lastAttemptTime != null) {
        if (currentTime - lastAttemptTime < COOLDOWN_MS) {
            return true; // ALERTE : Replay ou Double-clic détecté !
        } else {
            recentPayments.put(key, currentTime); // Rafraîchir le cooldown
            return false;
        }
    }
    return false;
}
```

---

### Protection Absolue contre les Injections SQL
Toutes les intéractions avec la base de données MySQL s'effectuent par le biais de requêtes paramétrées en utilisant l'objet standard JDBC **`PreparedStatement`**. 
- Aucune concaténation de chaînes de caractères provenant de l'entrée utilisateur n'est tolérée dans les requêtes SQL, ce qui neutralise à la source toute tentative d'injection SQL classique ou par aveugle.

#### Exemple de requête paramétrée ([UtilisateurDAO.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java#L472-L484)) :
```java
public String getPublicKeyByEmail(String email) throws SQLException {
    String sql = "SELECT public_key FROM utilisateur WHERE type_compte = 'ADMINISTRATEUR' AND email = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        // Remplacement du paramètre de manière sécurisée (échappement natif par le pilote JDBC)
        ps.setString(1, email.trim().toLowerCase());
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("public_key");
            }
        }
    }
    return null;
}
```

---

### Chiffrement des Données Sensibles At-Rest (Base de Données)
Pour assurer la conformité avec les réglementations de protection des données (RGPD/local) et appliquer le principe de la **Défense en Profondeur**, un plan d'action d'encryption au stockage (At-Rest) est élaboré.

#### Concept d'implémentation (`SensitiveDataCipher.java`) :
Afin de protéger les informations personnelles identifiables (PII) telles que l'adresse et le numéro de téléphone des clients dans la table `client`, une classe utilitaire de chiffrement symétrique applique les principes suivants :
- Elle extrait la clé d'encryption `STORAGE_AES_KEY` définie de manière centralisée dans le fichier de configuration `.env`.
- Elle effectue un chiffrement **AES-GCM-NoPadding** sur les chaînes de texte brut.
- Les valeurs chiffrées sont stockées en base de données précédées d'un marqueur distinctif `ENC::` (ex : `ENC::<base64_IV_et_ciphertext>`).
- **Rétrocompatibilité transparente** : Lors de la lecture d'un champ en base de données, si la valeur ne commence pas par `ENC::`, l'application la traite automatiquement comme du texte en clair hérité. Si elle commence par `ENC::`, elle extrait le bloc Base64, dérive l'IV, déchiffre le ciphertext avec la clé AES de stockage et renvoie le texte clair à la couche métier.
- Cela protège intégralement la confidentialité des données des clients même en cas de vol direct ou de fuite physique du fichier de base de données MySQL.
