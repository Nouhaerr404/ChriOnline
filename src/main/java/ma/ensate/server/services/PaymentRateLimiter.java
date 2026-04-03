package ma.ensate.server.services;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service pour empêcher les attaques par rejeu (Replay Attacks) et les doubles paiements intempestifs.
 */
public class PaymentRateLimiter {
    // Clé: "clientId_montant", Valeur: Timestamp du dernier essai
    private static final ConcurrentHashMap<String, Long> recentPayments = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 30000; // 30 secondes de blocage

    /**
     * Vérifie si l'utilisateur est en train de renvoyer la même demande de paiement.
     * @param clientId L'ID du client.
     * @param montant Le prix payé par le client.
     * @return true si c'est une attaque/rejeu, false sinon.
     */
    public static boolean isReplayAttack(String clientId, double montant) {
        String key = clientId + "_" + montant;
        long currentTime = System.currentTimeMillis();

        Long lastAttemptTime = recentPayments.putIfAbsent(key, currentTime);

        if (lastAttemptTime != null) {
            // Si le laps de temps est inférieur au temps de "Cooldown"
            if (currentTime - lastAttemptTime < COOLDOWN_MS) {
                return true; // ARRETER : Replay Attack ou Double Clic détecté !
            } else {
                // Le cooldown est passé, on met à jour avec le nouveau temps
                recentPayments.put(key, currentTime);
                return false;
            }
        }
        
        return false; // Première tentative
    }
}



// donc le client ne peux pas repayer avant que le COOLDOWN_MS s'ecoule cest ca ?


// Oui, c'est exactement ça ! Mais avec une petite subtilité très importante qu'il faut comprendre :

// Il ne peut pas repayer le MÊME MONTANT avant que les 30 secondes (ou la valeur dans COOLDOWN_MS) ne se soient écoulées.

// Étant donné que la clé du système de cache assemble l'ID du client et le montant exact de la commande (clientId_montant), voici ce qui se passe :

// Cas du doublon (Bloqué) : Si le client tente de payer 590 DH. S'il clique 5 fois par erreur sur le bouton à cause d'un lag, le serveur bloquera les 4 tentatives supplémentaires qui arriveront dans les 30 prochaines secondes pour ces 590 DH.
// Cas d'achat différent (Autorisé) : S'il paie une commande de 590 DH, et que juste après il décide de payer une autre commande au prix de 100 DH, ce sera autorisé immédiatement. La clé générée (clientId_100.0) sera différente de la première, donc il n'y aura pas de blocage !
// C'est voulu car cette approche permet de bloquer très efficacement les "doubles clics" ou les clics frénétiques (attaques), tout en laissant votre vrai client naviguer rapidement sans jamais le bloquer sur ses divers achats légitimes différents.