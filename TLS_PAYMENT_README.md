# Paiement securise avec TLS

Cette mise a jour ajoute un canal TLS dedie a la phase de paiement, dans un style volontairement proche du tutoriel.

## Ce qui a change

- Le client n'envoie plus le paiement sur le socket TCP general.
- La vue de paiement ouvre maintenant un `SSLSocket`.
- Le serveur principal demarre un petit serveur TLS dedie au paiement.
- Le certificat serveur est verifie cote client via un `TrustStore`.
- L'echange TLS utilise `BufferedReader` et `PrintWriter`, comme dans le tutoriel.
- La logique metier de paiement reste centralisee dans `PaymentService`.

## Ports utilises

- Serveur applicatif TCP : `SERVER_PORT`
- Serveur TLS paiement : `TLS_PAYMENT_PORT` (par defaut `9999`)

## Generation des certificats

Depuis la racine du projet :

```bash
mkdir -p tls
keytool -genkeypair -alias ecommerce -keyalg RSA -keysize 2048 -validity 3650 -keystore tls/server-keystore.jks -storepass 123456 -keypass 123456 -dname "CN=localhost, OU=ChriOnline, O=ENSA, L=Casablanca, ST=Casablanca-Settat, C=MA"
keytool -exportcert -alias ecommerce -keystore tls/server-keystore.jks -storepass 123456 -file tls/server.crt -rfc
keytool -importcert -alias server -file tls/server.crt -keystore tls/client-truststore.jks -storepass 123456 -noprompt
```

## Configuration

Les variables suivantes ont ete ajoutees dans `.env` et `.env.example` :

```env
TLS_PAYMENT_PORT=9999
TLS_KEYSTORE_PATH=tls/server-keystore.jks
TLS_KEYSTORE_PASSWORD=123456
TLS_TRUSTSTORE_PATH=tls/client-truststore.jks
TLS_TRUSTSTORE_PASSWORD=123456
```

## Flux

1. Le client construit une ligne de texte du type `commandeId=...;methode=...;cardLast4=...;token=...`.
2. `TLSPaymentClient` ouvre un `SSLSocket` et envoie cette ligne avec `PrintWriter`.
3. `TLSPaymentServer` lit la ligne avec `BufferedReader`.
4. Le serveur verifie la session utilisateur puis appelle `PaymentService`.
5. Le serveur renvoie une ligne de reponse via TLS.
