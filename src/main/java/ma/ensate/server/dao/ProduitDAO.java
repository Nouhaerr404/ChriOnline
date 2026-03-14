package ma.ensate.server.dao;

import ma.ensate.models.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProduitDAO {

    /**
     * Met à jour le stock d'un produit après un achat
     * Décrémente le stock de la quantité achetée
     */
    public boolean mettreAJourStock(int produitId, int quantiteAchetee) throws SQLException {
        String sql = "UPDATE produit SET stock = stock - ? WHERE id = ? AND stock >= ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantiteAchetee);
            stmt.setInt(2, produitId);
            stmt.setInt(3, quantiteAchetee);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupère un produit par son ID
     */
    public Produit findById(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Produit produit = new Produit();
                    produit.setId(rs.getInt("id"));
                    produit.setNom(rs.getString("nom"));
                    produit.setDescription(rs.getString("description"));
                    produit.setPrix(rs.getDouble("prix"));
                    produit.setStock(rs.getInt("stock"));
                    produit.setImageUrl(rs.getString("image_url"));
                    return produit;
                }
            }
        }
        return null;
    }

    /**
     * Vérifie si un produit a suffisamment de stock
     */
    public boolean verifierStock(int produitId, int quantite) throws SQLException {
        String sql = "SELECT stock FROM produit WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, produitId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int stock = rs.getInt("stock");
                    return stock >= quantite;
                }
            }
        }
        return false;
    }
}

