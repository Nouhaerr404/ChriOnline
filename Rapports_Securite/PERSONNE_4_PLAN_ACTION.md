# Personne 4 - Plan d'Action Appliqué

## Périmètre

La personne 4 est responsable de :

- l'intégration client-serveur du canal sécurisé (RSA + AES-GCM)
- la sécurisation du stockage des données sensibles (At-Rest)
- les tests de sécurité (Rejeu, Interception, Intégrité)
- la documentation finale et le guide de démonstration

## Travail appliqué

### 1. Intégration du canal sécurisé

- Intégration complète de `SecureChannel` dans [ClientTCP.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/client/network/ClientTCP.java)
- Handshake automatique (RSA) avant toute communication
- Intégration dans [ClientHandler.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/network/ClientHandler.java) avec validation de session

### 2. Sécurisation du stockage

- Implémentation de [SensitiveDataCipher.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/security/SensitiveDataCipher.java)
- Chiffrement AES-256 (GCM) des colonnes: `nom`, `email`, `adresse`, `tel`
- Mise à jour de [UtilisateurDAO.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/UtilisateurDAO.java)
- Mise à jour de [ClientDAO.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/dao/ClientDAO.java)
- **Rétrocompatibilité** : Les données non chiffrées restent lisibles. Les nouvelles écritures sont chiffrées automatiquement.

### 3. Tests de Sécurité

- [SensitiveDataCipherTest.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/test/java/ma/ensate/security/SensitiveDataCipherTest.java) : Valide le chiffrement/déchiffrement et la compatibilité.
- [SecureChannelTest.java](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/test/java/ma/ensate/security/SecureChannelTest.java) : 
    - **Round-trip** : Chiffrement/Déchiffrement fonctionnel.
    - **Anti-Rejeu** : Vérification qu'un message intercepté et renvoyé est rejeté.
    - **Intégrité** : Assurée par le tag GCM (AES-GCM).

## Configuration requise

La clé de stockage doit être définie dans `.env` :
```env
STORAGE_AES_KEY=votre-cle-secrete-ici
```

## Démonstration

Un guide de démonstration est disponible dans [PERSONNE_4_DEMO_GUIDE.md](file:///Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/PERSONNE_4_DEMO_GUIDE.md).
