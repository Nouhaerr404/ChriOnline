# Documentation Personne 3: Protocole de Handshake Sécurisé

## Résumé

Implémentation du protocole de handshake HTTPS-like pour ChriOnline.

**Technologie:**
- RSA 2048 bits (échange clés) - par Personne 1
- AES-256-GCM (chiffrement données) - par Personne 2
- Orchestration (SecureHandshake + SecureChannel) - par Personne 3

## Architecture

```
Phase 1: Handshake RSA
- Client demande clé publique RSA du serveur
- Serveur envoie clé publique
- Client génère clé AES-256 aléatoire
- Client chiffre clé AES avec clé publique du serveur
- Client envoie clé AES chiffrée
- Serveur déchiffre avec sa clé privée
→ Les deux partagent maintenant une clé AES secrète

Phase 2: Communication Sécurisée
- Toutes les Request/Response chiffrées avec AES-GCM
- IV unique par message
- Timestamp + nonce pour éviter rejeu
```

## Classes Créées

### HandshakeRequest.java
- Phase 1: demander clé publique
- Phase 2: envoyer clé AES chiffrée

### HandshakeResponse.java
- Phase 1: répondre avec clé publique RSA (Base64)
- Phase 2: confirmer "HANDSHAKE_COMPLETE"

### SecureHandshake.java
**Serveur:**
- `sendPublicKey(req)` - envoyer clé publique
- `receiveEncryptedAESKey(req)` - recevoir + déchiffrer AES

**Client:**
- `initiateHandshake()` - demander clé
- `sendEncryptedAESKey(resp, nonce)` - chiffrer + envoyer AES

### SecureChannel.java
- `writeSecureRequest(req)` - chiffrer requête
- `readSecureRequest()` - déchiffrer requête
- `writeSecureResponse(resp)` - chiffrer réponse
- `readSecureResponse()` - déchiffrer réponse

## Intégrations

### ClientHandler (Serveur)
- Handshake avant la boucle traiterRequete()
- Utilise SecureChannel pour chiffrer/déchiffrer

### ClientTCP (Client)
- Handshake dans connecter()
- Utilise SecureChannel pour chiffrer/déchiffrer

## Sécurité

✅ Protection rejeu (nonce + timestamp)
✅ Confidentialité (AES-256-GCM)
✅ Authentification serveur (RSA 2048)
✅ Intégrité (tag GCM 128 bits)
✅ IV unique par message

## Limitation Connue

⚠️ Perfect Forward Secrecy pas implémenté
⚠️ Clé AES réutilisée longtemps (amélioration: régénérer régulièrement)

## Notes pour Personne 4

✅ Personne 3 a préparé:
- Classes SecureHandshake + SecureChannel
- Intégration ClientHandler + ClientTCP
- Communication chiffrée complète

⚠️ Personne 4 peut:
- Ajouter chiffrement base données
- Améliorer Perfect Forward Secrecy
- Ajouter certificate pinning
- Intégrer certificats X.509

Pas de breaking changes!