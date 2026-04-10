# Rapport de Sécurité - Protection contre l'IP Spoofing

Ce document explique comment l'application **ChriOnline** est protégée contre les attaques de type IP Spoofing et Session Hijacking à travers le code implémenté.

## 1. Barrière Transport : L'IP Source Initiale
La première ligne de défense consiste à ne jamais faire confiance à l'adresse IP que le client pourrait déclarer. Nous extrayons l'adresse IP directement de la couche de transport TCP.

> [!TIP]
> En utilisant la classe `Socket` de Java, l'adresse IP est confirmée par le "3-way handshake" TCP, ce qui rend l'usurpation à distance extrêmement difficile.

### Code : [ClientHandler.java](file:///c:/ENSA/G%C3%A9nie%20Informatique/GI%202/S8/S%C3%A9curit%C3%A9%20Informatique/projets/src/main/java/ma/ensate/server/network/ClientHandler.java)
```java
// On récupère l'IP réelle identifiée par le système d'exploitation
clientIP = socket.getInetAddress().getHostAddress();
```

---

## 2. Barrière Réseau : Restriction d'Accès Administrateur
La règle de gestion impose que les administrateurs ne puissent se connecter que depuis un réseau interne sécurisé (VPN ou LAN).

### Code : [UserService.java](file:///c:/ENSA/G%C3%A9nie%20Informatique/GI%202/S8/S%C3%A9curit%C3%A9%20Informatique/projets/src/main/java/ma/ensate/server/services/UserService.java)
```java
private static boolean isInternalIP(String ip) {
    if (ip == null || ip.isBlank()) return false;
    return ip.startsWith("192.168.") // Classe C privée
            || ip.startsWith("10.") // Classe A privée
            || ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*") // Classe B privée
            || ip.equals("127.0.0.1") || ip.equals("::1"); // Localhost
}
```
> [!IMPORTANT]
> Un attaquant sur Internet avec une IP publique sera bloqué dès cette étape, même s'il possède les identifiants d'un administrateur.

---

## 3. Barrière Applicative : Session-IP Binding
C'est la protection la plus avancée. Elle empêche un pirate d'utiliser un jeton de session volé depuis une autre machine.

### Code : [SessionManager.java](file:///c:/ENSA/G%C3%A9nie%20Informatique/GI%202/S8/S%C3%A9curit%C3%A9%20Informatique/projets/src/main/java/ma/ensate/server/services/SessionManager.java)
```java
// Comparaison de l'IP actuelle du socket avec l'IP enregistrée au login
if (currentIP != null && !currentIP.equals(details.clientIP)) {
    logger.error("ALERTE SÉCURITÉ : Tentative de hijacking de session !");
    
    // ACTION RADICALE : Invalidation immédiate de la session
    activeSessions.remove(currentToken);
    return new SessionResult(false, null, "Alerte Sécurité : Votre IP a changé.");
}
```

> [!CAUTION]
> Si une tentative d'utilisation d'un jeton depuis une IP différente est détectée, le serveur ne se contente pas de refuser l'accès : **il détruit la session originale**, déconnectant ainsi l'utilisateur légitime par mesure de sécurité.

---

## Résumé de la Stratégie
1.  **Authenticité** : On utilise l'IP socket (TCP).
2.  **Périmètre** : On limite les administrateurs au réseau local.
3.  **Intégrité** : On lie le token de session à l'IP pour toute la durée de la connexion.
