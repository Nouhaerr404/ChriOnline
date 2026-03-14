package ma.ensate.server.dao;

import ma.ensate.models.Client;
import ma.ensate.models.Commande;
import ma.ensate.models.LigneCommande;
import ma.ensate.models.Produit;
import ma.ensate.models.StatutCommande;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeDAO {

    /**
     * Crée une nouvelle commande dans la base de données
     */
    public boolean creer(Commande commande) throws SQLException {
        String sql = "INSERT INTO commande (id, client_id, commande_date, statut, prix_a_payer) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Démarrer une transaction

            // Insérer la commande
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, commande.getId());
                stmt.setInt(2, commande.getClient().getId());
                stmt.setObject(3, commande.getCommandeDate());
                stmt.setString(4, commande.getStatut().name());
                stmt.setDouble(5, commande.getPrixAPayer());

                stmt.executeUpdate();
            }

            // Insérer les lignes de commande
            if (commande.getLignes() != null && !commande.getLignes().isEmpty()) {
                insererLignesCommande(conn, commande);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Insère les lignes de commande dans la base de données
     */
    private void insererLignesCommande(Connection conn, Commande commande) throws SQLException {
        String sql = "INSERT INTO ligne_commande (commande_id, produit_id, produit_nom, price_at_order, prix_ligne, quantite) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (LigneCommande ligne : commande.getLignes()) {
                stmt.setString(1, commande.getId());
                stmt.setInt(2, ligne.getProduit().getId());
                stmt.setString(3, ligne.getProduitNom());
                stmt.setDouble(4, ligne.getPriceAtOrder());
                stmt.setDouble(5, ligne.getPrixLigne());
                stmt.setInt(6, ligne.getQuantite());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Met à jour le statut d'une commande
     */
    public boolean mettreAJourStatut(String commandeId, StatutCommande nouveauStatut) throws SQLException {
        String sql = "UPDATE commande SET statut = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nouveauStatut.name());
            stmt.setString(2, commandeId);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Récupère une commande par son ID avec toutes ses lignes
     * CORRECTION: Jointure avec client et utilisateur
     */
    public Commande findById(String commandeId) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.id as user_id, u.nom, u.email, u.type_compte, " +
                "cl.adresse, cl.tel " +
                "FROM commande c " +
                "JOIN client cl ON c.client_id = cl.id " +
                "JOIN utilisateur u ON cl.id = u.id " +
                "WHERE c.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, commandeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Commande commande = new Commande();
                    commande.setId(rs.getString("id"));

                    // Créer le client avec les infos de utilisateur + client
                    Client client = new Client();
                    client.setId(rs.getInt("client_id")); // ou rs.getInt("user_id")
                    client.setNom(rs.getString("nom"));
                    client.setEmail(rs.getString("email"));
                    client.setAdresse(rs.getString("adresse")); // Maintenant disponible depuis client
                    client.setTel(rs.getString("tel")); // Maintenant disponible depuis client
                    commande.setClient(client);

                    // Date de commande
                    java.sql.Timestamp timestamp = rs.getTimestamp("commande_date");
                    if (timestamp != null) {
                        commande.setCommandeDate(timestamp.toLocalDateTime());
                    }

                    commande.setStatut(StatutCommande.valueOf(rs.getString("statut")));
                    commande.setPrixAPayer(rs.getDouble("prix_a_payer"));

                    // Charger les lignes de commande
                    commande.setLignes(chargerLignesCommande(conn, commandeId));

                    return commande;
                }
            }
        }
        return null;
    }

    /**
     * Charge les lignes de commande pour une commande donnée
     */
    private List<LigneCommande> chargerLignesCommande(Connection conn, String commandeId) throws SQLException {
        String sql = "SELECT lc.*, p.id as produit_id, p.nom as produit_nom_complet, p.description, p.prix, p.stock, p.image_url " +
                "FROM ligne_commande lc " +
                "LEFT JOIN produit p ON lc.produit_id = p.id " +
                "WHERE lc.commande_id = ?";

        List<LigneCommande> lignes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, commandeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LigneCommande ligne = new LigneCommande();
                    ligne.setId(rs.getInt("id"));

                    // Créer le produit
                    Produit produit = new Produit();
                    produit.setId(rs.getInt("produit_id"));
                    produit.setNom(rs.getString("produit_nom_complet"));
                    produit.setDescription(rs.getString("description"));
                    produit.setPrix(rs.getDouble("prix"));
                    produit.setStock(rs.getInt("stock"));
                    produit.setImageUrl(rs.getString("image_url"));
                    ligne.setProduit(produit);

                    ligne.setProduitNom(rs.getString("produit_nom"));
                    ligne.setPriceAtOrder(rs.getDouble("price_at_order"));
                    ligne.setPrixLigne(rs.getDouble("prix_ligne"));
                    ligne.setQuantite(rs.getInt("quantite"));

                    lignes.add(ligne);
                }
            }
        }
        return lignes;
    }

    /**
     * Récupère toutes les commandes d'un client (historique)
     * CORRECTION: Jointure avec client et utilisateur
     */
    public List<Commande> findByClientId(int clientId) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.id as user_id, u.nom, u.email, u.type_compte, " +
                "cl.adresse, cl.tel " +
                "FROM commande c " +
                "JOIN client cl ON c.client_id = cl.id " +
                "JOIN utilisateur u ON cl.id = u.id " +
                "WHERE c.client_id = ? " +
                "ORDER BY c.commande_date DESC";

        List<Commande> commandes = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Commande commande = new Commande();
                    commande.setId(rs.getString("id"));

                    // Créer le client avec les infos de utilisateur + client
                    Client client = new Client();
                    client.setId(rs.getInt("client_id"));
                    client.setNom(rs.getString("nom"));
                    client.setEmail(rs.getString("email"));
                    client.setAdresse(rs.getString("adresse"));
                    client.setTel(rs.getString("tel"));
                    commande.setClient(client);

                    // Date de commande
                    java.sql.Timestamp timestamp = rs.getTimestamp("commande_date");
                    if (timestamp != null) {
                        commande.setCommandeDate(timestamp.toLocalDateTime());
                    }

                    commande.setStatut(StatutCommande.valueOf(rs.getString("statut")));
                    commande.setPrixAPayer(rs.getDouble("prix_a_payer"));

                    // Charger les lignes de commande
                    commande.setLignes(chargerLignesCommande(conn, commande.getId()));

                    commandes.add(commande);
                }
            }
        }
        return commandes;
    }
}