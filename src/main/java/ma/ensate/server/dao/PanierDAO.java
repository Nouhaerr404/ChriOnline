package ma.ensate.server.dao;

import ma.ensate.models.LignePanier;
import ma.ensate.models.Panier;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * PanierDAO — toutes les opérations SQL sur les tables `panier` et `ligne_panier`.
 *
 * Conventions :
 *  - Chaque méthode ouvre/ferme sa propre connexion (via DBConnection).
 *  - Retourne null ou une liste vide en cas d'erreur (erreur loggée).
 *  - Les transactions (ex : vider le panier) utilisent setAutoCommit(false).
 */
public class PanierDAO {

    private static final Logger LOGGER = Logger.getLogger(PanierDAO.class.getName());

    // =========================================================================
    // PANIER — opérations principales
    // =========================================================================

    /**
     * Récupère le panier d'un client (sans les lignes).
     * Si le client n'a pas encore de panier, en crée un automatiquement.
     *
     * @param clientId  l'id du client (table utilisateur)
     * @return Panier existant ou nouvellement créé, null si erreur SQL
     */
    public Panier obtenirOuCreerPanier(int clientId) {
        String selectSql = "SELECT id, client_id, total FROM panier WHERE client_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {

            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToPanier(rs);
            }
            // Pas de panier trouvé → on en crée un
            return creerPanier(clientId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "obtenirOuCreerPanier(" + clientId + ")", e);
            return null;
        }
    }

    /**
     * Crée un nouveau panier vide pour le client donné.
     */
    private Panier creerPanier(int clientId) {
        String sql = "INSERT INTO panier (client_id, total) VALUES (?, 0.00)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, clientId);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return new Panier(keys.getInt(1), clientId, 0.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "creerPanier(" + clientId + ")", e);
        }
        return null;
    }

    /**
     * Met à jour le total du panier en base.
     */
    public boolean mettreAJourTotal(int panierId, double total) {
        String sql = "UPDATE panier SET total = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, total);
            ps.setInt(2, panierId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "mettreAJourTotal(panierId=" + panierId + ")", e);
            return false;
        }
    }

    // =========================================================================
    // LIGNES DU PANIER
    // =========================================================================

    /**
     * Récupère toutes les lignes d'un panier, avec le nom et le prix du produit.
     */
    public List<LignePanier> obtenirLignesDuPanier(int panierId) {
        String sql = """
                SELECT lp.id, lp.panier_id, lp.produit_id,
                       p.nom AS produit_nom, p.prix AS prix_unitaire,
                       lp.quantite, lp.subtotal
                FROM ligne_panier lp
                JOIN produit p ON p.id = lp.produit_id
                WHERE lp.panier_id = ?
                ORDER BY lp.id
                """;

        List<LignePanier> lignes = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, panierId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                lignes.add(mapRowToLignePanier(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "obtenirLignesDuPanier(panierId=" + panierId + ")", e);
        }
        return lignes;
    }

    /**
     * Ajoute un produit au panier ou incrémente la quantité s'il est déjà présent.
     * Met aussi à jour le subtotal de la ligne et le total du panier.
     *
     * @param panierId   id du panier
     * @param produitId  id du produit
     * @param quantite   quantité à ajouter (> 0)
     * @param prix       prix unitaire actuel du produit
     * @return true si l'opération a réussi
     */
    public boolean ajouterOuMettreAJourLigne(int panierId, int produitId,
                                             int quantite, double prix) {
        String checkSql = "SELECT id, quantite FROM ligne_panier WHERE panier_id=? AND produit_id=?";
        String insertSql = """
                INSERT INTO ligne_panier (panier_id, produit_id, quantite, subtotal)
                VALUES (?, ?, ?, ?)
                """;
        String updateSql = """
                UPDATE ligne_panier SET quantite=?, subtotal=?
                WHERE panier_id=? AND produit_id=?
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Vérifie si la ligne existe
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, panierId);
                    ps.setInt(2, produitId);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        // Ligne existante → mise à jour quantité
                        int nouvelleQte = rs.getInt("quantite") + quantite;
                        double subtotal  = prix * nouvelleQte;

                        try (PreparedStatement psUpd = conn.prepareStatement(updateSql)) {
                            psUpd.setInt(1, nouvelleQte);
                            psUpd.setDouble(2, subtotal);
                            psUpd.setInt(3, panierId);
                            psUpd.setInt(4, produitId);
                            psUpd.executeUpdate();
                        }
                    } else {
                        // Nouvelle ligne
                        try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                            psIns.setInt(1, panierId);
                            psIns.setInt(2, produitId);
                            psIns.setInt(3, quantite);
                            psIns.setDouble(4, prix * quantite);
                            psIns.executeUpdate();
                        }
                    }
                }

                // Recalcule et met à jour le total du panier
                double total = calculerTotalDepuisBase(conn, panierId);
                try (PreparedStatement psTotal = conn.prepareStatement(
                        "UPDATE panier SET total=? WHERE id=?")) {
                    psTotal.setDouble(1, total);
                    psTotal.setInt(2, panierId);
                    psTotal.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "ajouterOuMettreAJourLigne(panier=" + panierId
                    + " produit=" + produitId + ")", e);
            return false;
        }
    }

    /**
     * Modifie la quantité d'une ligne existante.
     * Si quantite <= 0, la ligne est supprimée.
     *
     * @param panierId  id du panier
     * @param produitId id du produit
     * @param quantite  nouvelle quantité (0 = suppression)
     * @param prix      prix unitaire actuel
     * @return true si OK
     */
    public boolean modifierQuantite(int panierId, int produitId, int quantite, double prix) {
        if (quantite <= 0) {
            return supprimerLigne(panierId, produitId);
        }

        String sql = """
                UPDATE ligne_panier SET quantite=?, subtotal=?
                WHERE panier_id=? AND produit_id=?
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, quantite);
                    ps.setDouble(2, prix * quantite);
                    ps.setInt(3, panierId);
                    ps.setInt(4, produitId);
                    ps.executeUpdate();
                }

                double total = calculerTotalDepuisBase(conn, panierId);
                try (PreparedStatement psTotal = conn.prepareStatement(
                        "UPDATE panier SET total=? WHERE id=?")) {
                    psTotal.setDouble(1, total);
                    psTotal.setInt(2, panierId);
                    psTotal.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "modifierQuantite(panier=" + panierId
                    + " produit=" + produitId + ")", e);
            return false;
        }
    }

    /**
     * Supprime une ligne du panier et recalcule le total.
     *
     * @param panierId  id du panier
     * @param produitId id du produit à retirer
     * @return true si au moins une ligne supprimée
     */
    public boolean supprimerLigne(int panierId, int produitId) {
        String sql = "DELETE FROM ligne_panier WHERE panier_id=? AND produit_id=?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, panierId);
                    ps.setInt(2, produitId);
                    rows = ps.executeUpdate();
                }

                double total = calculerTotalDepuisBase(conn, panierId);
                try (PreparedStatement psTotal = conn.prepareStatement(
                        "UPDATE panier SET total=? WHERE id=?")) {
                    psTotal.setDouble(1, total);
                    psTotal.setInt(2, panierId);
                    psTotal.executeUpdate();
                }

                conn.commit();
                return rows > 0;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "supprimerLigne(panier=" + panierId
                    + " produit=" + produitId + ")", e);
            return false;
        }
    }

    /**
     * Vide entièrement le panier (toutes les lignes) et remet total à 0.
     * Utilisé après validation d'une commande (appelé par Personne 4).
     */
    public boolean viderPanier(int panierId) {
        String deleteSql = "DELETE FROM ligne_panier WHERE panier_id=?";
        String resetSql  = "UPDATE panier SET total=0 WHERE id=?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setInt(1, panierId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(resetSql)) {
                    ps.setInt(1, panierId);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "viderPanier(panierId=" + panierId + ")", e);
            return false;
        }
    }

    // =========================================================================
    // Helpers privés
    // =========================================================================

    private double calculerTotalDepuisBase(Connection conn, int panierId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(subtotal), 0) AS total FROM ligne_panier WHERE panier_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        }
    }

    private Panier mapRowToPanier(ResultSet rs) throws SQLException {
        return new Panier(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getDouble("total")
        );
    }

    private LignePanier mapRowToLignePanier(ResultSet rs) throws SQLException {
        return new LignePanier(
                rs.getInt("id"),
                rs.getInt("panier_id"),
                rs.getInt("produit_id"),
                rs.getString("produit_nom"),
                rs.getDouble("prix_unitaire"),
                rs.getInt("quantite"),
                rs.getDouble("subtotal")
        );
    }
}