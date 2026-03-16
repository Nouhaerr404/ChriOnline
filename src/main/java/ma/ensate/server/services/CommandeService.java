package ma.ensate.server.services;

import ma.ensate.models.Client;
import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.StatutCommande;
import ma.ensate.server.dao.CommandeDAO;
import ma.ensate.server.dao.ProduitDAO;

import java.sql.SQLException;
import java.util.List;

public class CommandeService {

    private CommandeDAO commandeDAO;
    private ProduitDAO produitDAO;

    public CommandeService() {
        this.commandeDAO = new CommandeDAO();
        this.produitDAO = new ProduitDAO();
    }

    /**
     * Crée une nouvelle commande pour un client
     * Vérifie d'abord que tous les produits ont suffisamment de stock
     */
    public Commande creerCommande(Client client, List<LigneCommande> lignes)
            throws SQLException, IllegalArgumentException {
        // Validation du client
        if (client == null) {
            throw new IllegalArgumentException("Le client ne peut pas être null");
        }
        if (client.getId() <= 0) {
            throw new IllegalArgumentException("ID client invalide");
        }
        
        // Validation des lignes de commande si non vide 
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Une commande doit contenir au moins une ligne");
        }
        
        // Validation de chaque ligne
        for (LigneCommande ligne : lignes) {
            if (ligne == null) {
                throw new IllegalArgumentException("Une ligne de commande ne peut pas être null");
            }
            if (ligne.getProduit() == null) {
                throw new IllegalArgumentException("Le produit d'une ligne ne peut pas être null");
            }
            if (ligne.getQuantite() <= 0) {
                throw new IllegalArgumentException("La quantité doit être supérieure à 0");
            }
            if (ligne.getQuantite() > 1000) {
                throw new IllegalArgumentException("La quantité ne peut pas dépasser 1000 unités");
            }
            if (ligne.getProduit().getPrix() < 0) {
                throw new IllegalArgumentException("Le prix du produit ne peut pas être négatif");
            }
            
            // Vérifier le stock
            if (!produitDAO.verifierStock(ligne.getProduit().getId(), ligne.getQuantite())) {
                throw new IllegalArgumentException(
                        "Stock insuffisant pour le produit: " + ligne.getProduit().getNom() +
                                " (demandé: " + ligne.getQuantite() + ")");
            }
        }

        // Créer la commande
        Commande commande = new Commande(client);
        commande.setLignes(lignes);
        commande.calculerTotal();
        
        // Validation du montant total
        if (commande.getPrixAPayer() <= 0) {
            throw new IllegalArgumentException("Le montant total de la commande doit être supérieur à 0");
        }
        if (commande.getPrixAPayer() > 1000000) {
            throw new IllegalArgumentException("Le montant total ne peut pas dépasser 1 000 000 MAD");
        }

        // Sauvegarder en base de données
        if (commandeDAO.creer(commande)) {
            return commande;
        } else {
            throw new SQLException("Erreur lors de la création de la commande");
        }
    }

    /**
     * Valide une commande (change le statut de EN_ATTENTE à VALIDE)
     * Cette méthode permet de valider une commande manuellement
     */
    public boolean validerCommande(String commandeId) throws SQLException {
        // Validation de l'ID
        if (commandeId == null || commandeId.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la commande ne peut pas être vide");
        }
        
        Commande commande = commandeDAO.findById(commandeId);

        if (commande == null) {
            throw new IllegalArgumentException("Commande introuvable: " + commandeId);
        }

        // Vérifier que la commande est en attente
        if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
            throw new IllegalStateException(
                    "Impossible de valider une commande avec le statut: " + commande.getStatut());
        }
        
        // Vérifier que la commande a des lignes
        if (commande.getLignes() == null || commande.getLignes().isEmpty()) {
            throw new IllegalStateException("Impossible de valider une commande sans lignes");
        }

        // Mettre à jour le statut
        return commandeDAO.mettreAJourStatut(commandeId, StatutCommande.VALIDE);
    }

    /**
     * Change le statut d'une commande (gestion des différents états)
     * Permet de faire évoluer une commande: EN_ATTENTE -> VALIDE -> EXPEDIE ->
     * LIVRE
     */
    public boolean changerStatutCommande(String commandeId, StatutCommande nouveauStatut) throws SQLException {
        // Validation de l'ID
        if (commandeId == null || commandeId.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la commande ne peut pas être vide");
        }
        
        // Validation du statut
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le nouveau statut ne peut pas être null");
        }
        
        Commande commande = commandeDAO.findById(commandeId);

        if (commande == null) {
            throw new IllegalArgumentException("Commande introuvable: " + commandeId);
        }

        // Vérifier la transition de statut est valide
        if (!estTransitionValide(commande.getStatut(), nouveauStatut)) {
            throw new IllegalStateException(
                    "Transition invalide de " + commande.getStatut() + " vers " + nouveauStatut);
        }

        return commandeDAO.mettreAJourStatut(commandeId, nouveauStatut);
    }

    /**
     * Vérifie si une transition de statut est valide
     * Règles: EN_ATTENTE -> VALIDE -> EXPEDIE -> LIVRE
     */
    private boolean estTransitionValide(StatutCommande statutActuel, StatutCommande nouveauStatut) {
        // On peut toujours rester au même statut (pour les mises à jour)
        if (statutActuel == nouveauStatut) {
            return true;
        }

        // Transitions autorisées
        if (statutActuel == StatutCommande.EN_ATTENTE) {
            return nouveauStatut == StatutCommande.VALIDE;
        }
        
        if (statutActuel == StatutCommande.VALIDE) {
            return nouveauStatut == StatutCommande.EXPEDIE;
        }
        
        if (statutActuel == StatutCommande.EXPEDIE) {
            return nouveauStatut == StatutCommande.LIVRE;
        }
        
        // Une commande livrée ne peut plus changer de statut
        if (statutActuel == StatutCommande.LIVRE) {
            return false;
        }
        
        // Cas par défaut
        return false;
    }

    /**
     * Récupère l'historique des commandes d'un client
     * Permet d'afficher toutes les commandes passées par un client
     */
    public List<Commande> getHistorique(int clientId) throws SQLException {
        if (clientId <= 0) {
            throw new IllegalArgumentException("ID client invalide");
        }
        return commandeDAO.findByClientId(clientId);
    }

    /**
     * Récupère une commande par son ID
     */
    public Commande getCommandeById(String commandeId) throws SQLException {
        if (commandeId == null || commandeId.trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID de la commande ne peut pas être vide");
        }
        return commandeDAO.findById(commandeId);
    }
}