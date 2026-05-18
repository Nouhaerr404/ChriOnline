# DOCUMENTATION D'ARCHITECTURE & FLUX DE SÉCURITÉ
## Projet ChriOnline — Cartographie et Flux de Sécurité

> [!NOTE]
> Cette documentation présente l'architecture de sécurité multi-couches de l'application **ChriOnline** et détaille graphiquement les protocoles d'échange réseau sous forme de diagrammes de séquence Mermaid.

---

## 1. Vue d'Ensemble de l'Architecture

ChriOnline adopte une architecture client/serveur robuste reposant sur des **Sockets TCP persistantes**, des **Sockets SSL/TLS mutuellement authentifiées**, et une base de données **MySQL** pour la persistance des données. 

La sécurité du système est structurée selon le modèle de la **Défense en Profondeur**, répartie sur 6 couches logiques distinctes pour garantir qu'aucune vulnérabilité isolée ne puisse compromettre l'intégralité du système.

---

## 2. Matrice de Sécurité Multi-Couches

| Couche Logique | Menaces Atténuées | Mécanismes & Algorithmes | Classes Clés Responsables |
| :--- | :--- | :--- | :--- |
| **1. Réseau & Pare-feu** | SYN Flood, Déni de Service (DoS), Spoofing d'IP | Limiteur de connexions par IP, nettoyage des sockets éphémères pendantes, cookies cryptographiques. | [SYNFloodProtection.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/security/SYNFloodProtection.java)<br>[SYNCookieManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/security/SYNCookieManager.java) |
| **2. Transport & Tunnel** | Interception (MITM), Eavesdropping sur transactions bancaires | SSL/TLS v1.3 strict, Authentification mutuelle client-serveur (mTLS) par certificats. | [TLSPaymentServer.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TLSPaymentServer.java) |
| **3. Session & Confidentialité** | Usurpation de session (Hijacking), Rejeu de paquets réseau | Handshake hybride RSA-2048 / AES-256, chiffrement symétrique AES-GCM-128, nonces d'unicité (UUID), timestamps de fraîcheur. | [SecureHandshake.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/SecureHandshake.java)<br>[SecureChannel.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/SecureChannel.java) |
| **4. Authentification & MFA** | Force brute, Usurpation d'identités, Scripts automatiques (Spambots) | Double authentification (MFA) via OTP par email, challenge-response asymétrique RSA (clés PEM), protection anti-brute force à verrouillage incrémental, CAPTCHA AWT distordu avec Sliding-Window Rate Limiting. | [AdminAuthService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/AdminAuthService.java)<br>[OtpStore.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/OtpStore.java)<br>[CaptchaService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/CaptchaService.java)<br>[UtilisateurDAO.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java) |
| **5. Autorisation & Métier** | BOLA/IDOR (Broken Object Level Authorization), Double-clic de paiement accidentel | Liaison IP-Session, contrôle d'IP admin (réseau interne), validation systématique de la correspondance Token <-> ID Cible, limiteur de double-paiement (cooldown 30s). | [ClientHandler.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/network/ClientHandler.java)<br>[SessionManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/SessionManager.java)<br>[PaymentRateLimiter.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/services/PaymentRateLimiter.java) |
| **6. Données & Intégrité** | Fuite physique de base de données, Injection SQL, Non-répudiation | Requêtes JDBC paramétrées `PreparedStatement`, hachage BCrypt (coût 12) avec migration active transparente, signatures de reçus de transactions SHA256withRSA via KeyStore Java (PKCS12), chiffrement AES-GCM At-Rest. | [KeyStoreManager.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/KeyStoreManager.java)<br>[DigitalSignatureService.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/security/DigitalSignatureService.java)<br>[UtilisateurDAO.java](file:///c:/GI2-2/S%C3%A9curit%C3%A9%20Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java) |

---

## 3. Flux et Protocoles de Sécurité (Mermaid Sequence Diagrams)

### Diagramme 1 : Protocole du Handshake Hybride (RSA + AES)
Ce flux détaille l'initialisation éphémère du canal de communication chiffré à chaque nouvelle socket.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (JavaFX)
    participant Serveur as Serveur ChriOnline
    participant RSA as RSA Key Manager

    Client->>Serveur: Demande de Handshake (GET_SERVER_PUBLIC_KEY) avec nonce_client
    Serveur->>RSA: Obtenir la clé publique RSA du serveur
    RSA-->>Serveur: Clé publique RSA (Base64)
    Serveur-->>Client: Réponse publique (Clé RSA serveur + signature)
    
    Note over Client: Client valide le nonce<br/>Génère une clé éphémère AES-256
    Client->>Client: Chiffre la clé AES avec la clé RSA publique du serveur
    
    Client->>Serveur: Envoie la clé AES chiffrée (SEND_ENCRYPTED_AES)
    Serveur->>Serveur: Déchiffre avec sa clé privée RSA
    Serveur-->>Client: HANDSHAKE_COMPLETE (Canal Chiffré Établi)
```

---

### Diagramme 2 : Transmission sur Canal Sécurisé & Protection Anti-Rejeu
Ce flux illustre comment chaque requête métier est chiffrée et protégée contre les interceptions et les attaques par rejeu.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (JavaFX)
    participant Channel as Secure Channel (AES-GCM)
    participant Serveur as Serveur ChriOnline

    Note over Client: Prépare une requête métier (Ex: AFFICHER_PANIER)
    Client->>Channel: Message clair + Nonce UUID + Timestamp
    Note over Channel: Chiffre le message avec la clé AES de session<br/>Génère IV + Tag d'Intégrité GCM
    Channel->>Serveur: Transmet le flux chiffré (IV + Ciphertext + Tag)
    
    Note over Serveur: Déchiffre le message avec la clé AES<br/>Valide l'intégrité via le tag GCM
    Serveur->>Serveur: Vérifie la fraîcheur temporelle (Timestamp < 60s)
    Serveur->>Serveur: Vérifie l'unicité du Nonce dans usedNonces Map
    
    alt Nonce déjà consommé OU Timestamp expiré
        Serveur-->>Client: Rejet (SecurityException)
    else Validation Réussie
        Serveur->>Serveur: Traite la requête (Afficher panier)
        Serveur-->>Client: Réponse chiffrée en AES-GCM
    end
```

---

### Diagramme 3 : Authentification Passwordless des Administrateurs
Ce protocole montre comment un administrateur s'authentifie à l'aide de sa clé privée locale sans jamais transmettre de mot de passe sur le réseau.

```mermaid
sequenceDiagram
    autonumber
    participant Admin as Admin (JavaFX)
    participant Serveur as Serveur ChriOnline
    participant BDD as Base de données MySQL

    Admin->>Serveur: Demande de Challenge (GENERATE_CHALLENGE_ADMIN) avec email_admin
    Serveur->>BDD: Rechercher si l'email correspond à un Admin
    BDD-->>Serveur: Administrateur trouvé
    Serveur->>Serveur: Génère un challenge aléatoire UUID (TTL: 30s)
    Serveur-->>Admin: Challenge généré (Base64)

    Note over Admin: Charge la clé privée PEM locale<br/>Signe le challenge avec RSA (SHA256withRSA)
    Admin->>Serveur: Envoie (VERIFY_SIGNATURE_ADMIN) avec challenge + signature + email
    
    Serveur->>Serveur: Récupère et supprime immédiatement le challenge de la mémoire (Anti-Rejeu)
    Serveur->>BDD: Récupérer la clé publique RSA de l'admin
    BDD-->>Serveur: Clé publique RSA (Base64)
    
    Serveur->>Serveur: Valide la signature avec la clé publique
    
    alt Signature valide & Challenge non expiré
        Serveur->>Serveur: Génère un token de session<br/>Enregistre l'association Token <-> IP Admin
        Serveur-->>Admin: Connexion autorisée + Token
    else Échec validation
        Serveur-->>Admin: Accès refusé
    end
```

---

### Diagramme 4 : Double Authentification (MFA/2FA par OTP Email)
Ce flux décrit le second facteur d'authentification requis pour les comptes clients.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (JavaFX)
    participant Serveur as Serveur ChriOnline
    participant Mail as Service Email SMTP
    participant BDD as Base de données MySQL

    Client->>Serveur: Connexion (LOGIN) avec email + password + captcha
    Serveur->>BDD: Vérifier l'authentification (BCrypt)
    BDD-->>Serveur: Identifiants corrects (MFA activé)
    
    Serveur->>Serveur: Génère un code OTP à 6 chiffres (TTL: 5 min)
    Serveur->>Mail: Envoyer code OTP (Asynchrone)
    Mail-->>Client: Email reçu avec le code OTP
    Serveur-->>Client: REQUIRES_2FA (Bascule de l'IHM)

    Client->>Serveur: Envoyer Code OTP (VERIFY_2FA)
    Serveur->>Serveur: Valide l'OTP en mémoire (OtpStore)
    
    alt OTP correct & valide
        Serveur->>Serveur: Détruit l'OTP pour empêcher le rejeu
        Serveur->>Serveur: Initialise la Session & Enregistre l'IP client
        Serveur-->>Client: Connexion réussie !
    else OTP incorrect ou expiré
        Serveur-->>Client: Échec OTP
    end
```

---

### Diagramme 5 : Protection Anti-Bot & Rate Limiting CAPTCHA
Ce schéma illustre la double protection CAPTCHA visuelle et le rate-limiting réseau glissant empêchant les attaques par déni de service.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (JavaFX)
    participant Serveur as Serveur ChriOnline
    participant RateLimiter as Sliding Window Limiter

    Client->>Serveur: Demande un nouveau CAPTCHA (GET_CAPTCHA_NEW)
    Serveur->>RateLimiter: Évaluer la fréquence pour l'IP du client
    
    alt Requêtes > 5 en 10 secondes
        RateLimiter-->>Serveur: Limite dépassée
        Serveur-->>Client: Erreur 429 (Trop de requêtes, attendez 10s)
    else Autorisé
        RateLimiter->>Serveur: Requête autorisée
        Serveur->>Serveur: Génère le texte du CAPTCHA (Ex: A8b9X)
        Serveur->>Serveur: AWT : Dessine l'image avec bruit, lignes et distorsions
        Serveur-->>Client: Renvoie l'image en Base64 + Token de session du CAPTCHA
    end

    Client->>Serveur: Soumet l'action (Ex: REGISTER) + texte saisi + token
    Serveur->>Serveur: Valide de manière atomique le CAPTCHA
    
    alt Saisie correcte
        Serveur->>Serveur: Supprime le CAPTCHA (Validation unique)
        Serveur-->>Client: Action complétée
    else Saisie incorrecte
        Serveur->>Serveur: Incrémente le compteur d'essais (Max 3)
        alt Essais = 3
            Serveur->>Serveur: Détruit la session du CAPTCHA
        end
        Serveur-->>Client: Code CAPTCHA incorrect ou expiré
    end
```

---

### Diagramme 6 : Validation Réseau (SYN Cookies & Flood Protection)
Ce flux présente comment le serveur valide la socket avant d'allouer des ressources thread complexes.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client TCP Socket
    participant Serveur as Serveur ChriOnline (TCP Engine)
    participant Cookie as SYN Cookie Manager
    participant Flood as SYN Flood Protection

    Client->>Serveur: Tentative de Connexion TCP (SYN)
    Serveur->>Flood: Valider la limite de connexions pour l'IP
    
    alt IP déjà à la limite (MAX: 100)
        Flood-->>Serveur: Rejeter connexion
        Serveur-->>Client: Fermeture de la socket
    else Sous le seuil
        Flood->>Cookie: Générer un cookie cryptographique temporaire
        Cookie-->>Serveur: Cookie généré (Hash IP + Port + Secret)
        Serveur->>Serveur: Enregistre la socket en attente éphémère (TTL 10s)
        Serveur-->>Client: Autorise la socket à démarrer le Handshake Hybride
    end
```

---

## 4. Flux Applicatifs standards avec Contrôles de Sécurité

### Diagramme 7 : Création de Commande & Paiement (BOLA + Cooldown mTLS)
Ce flux intègre la validation d'autorisation métier (BOLA/IDOR) et la liaison avec la passerelle bancaire TLS.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (JavaFX)
    participant Serveur as Serveur ChriOnline
    participant BDD as Base de données MySQL
    participant Limit as Payment Rate Limiter
    participant Bank as Serveur de Paiement TLS (mTLS)

    %% Partie 1: Création Commande (BOLA Protection)
    Client->>Serveur: Créer commande (CREER_COMMANDE) avec clientId + Lignes
    Note over Serveur: Évaluation BOLA / IDOR :<br/>Récupère le userId associé au Token de session
    
    alt userId != clientId de la requête
        Serveur-->>Client: Erreur d'autorisation (Tentative IDOR déjouée)
    else Autorisé
        Serveur->>BDD: Enregistrer la commande (Statut: PENDANTE)
        BDD-->>Serveur: Commande enregistrée
        Serveur-->>Client: Commande créée avec succès
    end

    %% Partie 2: Paiement (mTLS + Cooldown)
    Client->>Serveur: Effectuer Paiement (EFFECTUER_PAIEMENT) pour commandeId
    Serveur->>BDD: Récupérer la commande par ID
    BDD-->>Serveur: Commande trouvée (Client propriétaire: clientId, Montant: X)
    
    Note over Serveur: Évaluation BOLA : vérifie que la commande appartient bien au client connecté
    
    Serveur->>Limit: Vérifier le cooldown de paiement (clientId, Montant)
    
    alt Requête identique soumise dans les dernières 30 secondes
        Limit-->>Serveur: Rejeu détecté (Double-clic ou trame dupliquée)
        Serveur-->>Client: Requête de paiement déjà en cours. Veuillez patienter 30s.
    else Cooldown Autorisé
        Note over Serveur: Établit une socket SSL/TLS v1.3 avec le serveur de paiement<br/>Présente son certificat client (mTLS)
        Serveur->>Bank: Traiter transaction (CommandeId, Montant)
        Bank->>Bank: Valide le certificat du serveur ChriOnline
        Bank-->>Serveur: Paiement validé (Receipt signé SHA256withRSA)
        
        Serveur->>BDD: Mettre à jour la commande (Statut: VALIDE)
        BDD-->>Serveur: Commande mise à jour
        Serveur-->>Client: Paiement approuvé & Reçu sécurisé
    end
```
