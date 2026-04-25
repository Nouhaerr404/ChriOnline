# Paiement Securise avec TLS

Ce document explique la logique complete du paiement securise avec TLS dans le projet **ChriOnline**, en restant conforme a l'esprit et a la structure du tutoriel du professeur.

## 1. Objectif

Le but est de **securiser la phase de paiement** dans l'application desktop Java en utilisant :

- `SSLSocket` cote client
- `SSLServerSocket` cote serveur
- un `keystore` pour le certificat serveur
- un `truststore` pour verifier ce certificat cote client

Ainsi, les donnees de paiement ne transitent plus en clair sur le reseau.

## 2. Principe general

Le paiement suit la logique du tutoriel :

1. Le client ouvre une connexion TLS vers le serveur de paiement
2. Le serveur presente son certificat
3. Le client verifie ce certificat via le `truststore`
4. La session TLS est etablie
5. Les informations de paiement sont envoyees de maniere chiffree

Dans notre projet, cela veut dire que :

- le reste de l'application continue a utiliser le canal TCP habituel
- **seule la phase de paiement** passe par un canal TLS dedie

## 3. Conformite avec le tutoriel du prof

Le tutoriel du professeur impose une structure simple :

- `System.setProperty(...)`
- `SSLServerSocketFactory.getDefault()`
- `SSLSocket`
- `BufferedReader`
- `PrintWriter`
- envoi d'une ligne de texte

Cette structure est bien respectee dans le projet.

### Cote serveur

La logique TLS serveur se trouve dans :

- [TLSPaymentServer.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TLSPaymentServer.java)

On y retrouve :

- la definition du `keystore`
- la creation du `SSLServerSocket`
- l'acceptation de la connexion TLS
- la lecture du paiement avec `BufferedReader`
- le traitement du paiement
- l'envoi d'une reponse texte avec `PrintWriter`

### Cote client

La logique TLS client se trouve dans :

- [TLSPaymentClient.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/client/network/TLSPaymentClient.java)

On y retrouve :

- la definition du `truststore`
- l'ouverture d'un `SSLSocket`
- l'envoi d'une ligne texte representant le paiement
- la lecture de la reponse du serveur

## 4. Fichiers impliques dans le projet

### Fichiers principaux

- [TLSPaymentServer.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TLSPaymentServer.java)
- [TLSPaymentClient.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/client/network/TLSPaymentClient.java)
- [PaiementView.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/client/views/PaiementView.java)
- [PaymentService.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/services/PaymentService.java)
- [TCPServer.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TCPServer.java)
- [paiement.fxml](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/resources/ma/ensate/fxml/paiement.fxml)

### Fichiers de configuration

- [.env](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/.env)
- [.env.example](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/.env.example)
- [TLS_PAYMENT_README.md](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/TLS_PAYMENT_README.md)

### Fichiers TLS generes

- `tls/server-keystore.jks`
- `tls/server.crt`
- `tls/client-truststore.jks`

## 5. Logique detaillee du serveur TLS

### 5.1 Chargement du certificat serveur

Dans [TLSPaymentServer.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TLSPaymentServer.java), le serveur configure :

- `javax.net.ssl.keyStore`
- `javax.net.ssl.keyStorePassword`

Cela permet a Java d'utiliser le certificat stocke dans le `keystore`.

### 5.2 Creation du serveur TLS

Le serveur cree ensuite :

- un `SSLServerSocketFactory`
- puis un `SSLServerSocket` sur le port `9999`

Le port est configurable dans `.env` avec :

```env
TLS_PAYMENT_PORT=9999
```

### 5.3 Attente d'une connexion cliente

Le serveur attend qu'un client se connecte :

- `serverSocket.accept()`

Quand le client arrive, un `SSLSocket` est cree pour cette session.

### 5.4 Reception du paiement en texte

Conformement au tutoriel, le serveur lit une ligne texte avec :

- `BufferedReader`

Exemple de ligne recue :

```text
commandeId=CMD123;methode=CARTE_BANCAIRE;cardLast4=1111;token=abc123
```

Cette ligne est ensuite decodee dans le serveur.

### 5.5 Verification de la session utilisateur

Avant de traiter le paiement, le serveur verifie le `token` utilisateur via :

- [SessionManager.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/services/SessionManager.java)

Cela evite qu'une personne non connectee puisse utiliser le canal TLS de paiement.

### 5.6 Traitement du paiement

Le vrai traitement est delegue a :

- [PaymentService.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/services/PaymentService.java)

Ce service :

- valide la requete
- verifie la methode de paiement
- verifie que la commande existe
- applique la protection anti-rejeu
- enregistre le paiement
- met a jour le statut de la commande
- vide le panier du client
- envoie la notification UDP si necessaire

### 5.7 Reponse du serveur

Le serveur renvoie ensuite une ligne texte avec `PrintWriter`.

Exemple :

```text
status=OK;message=Paiement%20effectue%20avec%20succes;newToken=
```

ou

```text
status=ERROR;message=Commande%20introuvable;newToken=
```

## 6. Logique detaillee du client TLS

### 6.1 Chargement du truststore

Dans [TLSPaymentClient.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/client/network/TLSPaymentClient.java), le client configure :

- `javax.net.ssl.trustStore`
- `javax.net.ssl.trustStorePassword`

Le `truststore` contient le certificat du serveur deja importe.

### 6.2 Ouverture de la connexion TLS

Le client ouvre un `SSLSocket` vers :

- l'hote `localhost`
- le port `9999`

Cette connexion etablit un canal chiffre pour le paiement.

### 6.3 Construction de la ligne de paiement

Le client construit ensuite une ligne texte contenant :

- `commandeId`
- `methode`
- `cardLast4`
- `token`

Cette ligne est envoyee avec `PrintWriter`.

### 6.4 Lecture de la reponse

Le client lit la reponse du serveur avec `BufferedReader`, puis reconstruit un objet `Response`.

## 7. Integration dans l'interface graphique

L'integration utilisateur se fait dans :

- [PaiementView.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/client/views/PaiementView.java)
- [paiement.fxml](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/resources/ma/ensate/fxml/paiement.fxml)

Quand l'utilisateur clique sur le bouton :

- `CONFIRMER ET PAYER VIA TLS`

la vue appelle :

- `TLSPaymentClient.effectuerPaiement(...)`

L'interface affiche aussi un indicateur visuel :

- `TLS ACTIF`

pour montrer clairement que le paiement part via le canal securise TLS.

## 8. Demarrage du serveur TLS

Le serveur TLS n'est pas lance separement a la main : il est demarre automatiquement par :

- [TCPServer.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/server/network/TCPServer.java)

Ainsi, quand on lance le serveur principal, on lance aussi :

- le serveur TCP classique
- le serveur TLS dedie au paiement

## 9. Generation des certificats

La generation suit le principe du tutoriel avec `keytool`.

Les commandes sont documentees dans :

- [TLS_PAYMENT_README.md](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/TLS_PAYMENT_README.md)

Resume des commandes :

```bash
mkdir -p tls
keytool -genkeypair -alias ecommerce -keyalg RSA -keysize 2048 -validity 3650 -keystore tls/server-keystore.jks -storepass 123456 -keypass 123456 -dname "CN=localhost, OU=ChriOnline, O=ENSA, L=Casablanca, ST=Casablanca-Settat, C=MA"
keytool -exportcert -alias ecommerce -keystore tls/server-keystore.jks -storepass 123456 -file tls/server.crt -rfc
keytool -importcert -alias server -file tls/server.crt -keystore tls/client-truststore.jks -storepass 123456 -noprompt
```

## 10. Configuration dans `.env`

Les variables ajoutees sont :

```env
TLS_PAYMENT_PORT=9999
TLS_KEYSTORE_PATH=tls/server-keystore.jks
TLS_KEYSTORE_PASSWORD=123456
TLS_TRUSTSTORE_PATH=tls/client-truststore.jks
TLS_TRUSTSTORE_PASSWORD=123456
```

Ces valeurs sont lues grace a :

- [ConfigLoader.java](/Users/mac/Documents/School/S8/Sécurité Informatique/ChriOnline/src/main/java/ma/ensate/util/ConfigLoader.java)

## 11. Ce qu'on a respecte exactement du tutoriel

Nous avons respecte :

- l'utilisation de TLS avec `SSLSocket` et `SSLServerSocket`
- l'usage d'un `keystore` cote serveur
- l'usage d'un `truststore` cote client
- la verification du certificat serveur
- l'utilisation de `System.setProperty(...)`
- la lecture et l'ecriture en texte avec `BufferedReader` et `PrintWriter`
- l'envoi du paiement sous forme d'une ligne texte

## 12. Adaptation au projet ChriOnline

Le tutoriel du prof donne un exemple minimal :

```text
amount=100;token=abc123
```

Dans notre projet, nous avons adapte cette ligne aux besoins reels de l'application :

```text
commandeId=...;methode=...;cardLast4=...;token=...
```

Cette adaptation est normale car notre application doit :

- identifier la commande
- savoir quel type de paiement a ete choisi
- transmettre les 4 derniers chiffres de la carte
- verifier la session utilisateur

## 13. Avantages securite obtenus

Avec cette implementation :

- les donnees de paiement sont chiffrees pendant le transport
- le client verifie qu'il parle bien au bon serveur
- le risque d'interception reseau est reduit
- le paiement est isole sur un canal dedie plus securise

## 14. Conclusion

Le paiement securise TLS de **ChriOnline** suit la logique du tutoriel du professeur, tout en restant integre proprement dans l'architecture du projet.

En resume :

- le client ouvre une connexion TLS
- le serveur presente son certificat
- le client le verifie avec le `truststore`
- les donnees de paiement sont envoyees sous forme de texte chiffre
- le serveur traite ensuite le paiement via la logique metier du projet

Cette solution permet d'avoir un **paiement securise, simple, demonstrable, et conforme au tutoriel**.
