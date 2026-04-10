# Test SYN Flood Protection

## Comment tester la protection SYN Flood

### 1. Démarrer le serveur ChriOnline
```bash
cd c:\GI2-2\Sécurité Informatique\ChriOnline
mvn compile exec:java -Dexec.mainClass="ma.ensate.server.network.TCPServer"
```

### 2. Lancer le simulateur d'attaque
```bash
mvn compile exec:java -Dexec.mainClass="ma.ensate.test.SynFloodSimulator"
```

### 3. Observer les logs de protection

Dans la console du serveur, vous verrez :
- **Connexions autorisées** : `Connexion autorisée pour IP: 127.0.0.1, Total en attente: X`
- **Rate limiting** : `IP 127.0.0.1 bloquée - limite de connexions dépassée (100/100)`
- **Nettoyage automatique** : `Nettoyage terminé - X connexions expirées supprimées`
- **Statistiques périodiques** : `Nettoyage périodique - Connexions en attente: X, Cookies actifs: Y`

### 4. Paramètres de test modifiables

Dans `SynFloodSimulator.java` :
- `NB_CONNEXIONS = 200` : Nombre de connexions à simuler
- `PORT = 5001` : Port du serveur (doit correspondre à SERVER_PORT dans .env)
- `Thread.sleep(30_000)` : Durée de maintien des connexions (30 secondes)

### 5. Comportement attendu

1. **Premières connexions** : Acceptées jusqu'à 100 par IP
2. **Rate limiting activé** : Au-delà de 100, les connexions sont rejetées
3. **Timeout automatique** : Après 10 secondes, les connexions inactives sont supprimées
4. **Nettoyage périodique** : Toutes les 30 secondes, le système nettoie les connexions expirées

### 6. Logs de monitoring

Les logs montrent en temps réel :
- Nombre de connexions en attente
- IP bloquées pour rate limiting
- Cookies SYN générés et validés
- Statistiques de nettoyage

### 7. Test avec vrais clients

Pendant l'attaque simulée, essayez de vous connecter avec un client normal :
- Les connexions légitimes devraient être acceptées si sous la limite
- Les logs montreront la différence entre attaque et trafic normal
