package ma.ensate.server.dao;

import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
     * Récupère tous les produits
     */
    public List<Produit> findAll() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, c.nom as cat_nom FROM produit p LEFT JOIN categorie c ON p.categorie_id = c.id";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
        }
        return produits;
    }

    /**
     * Récupère tous les produits d'une catégorie
     */
    public List<Produit> findByCategorie(int categoryId) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, c.nom as cat_nom FROM produit p LEFT JOIN categorie c ON p.categorie_id = c.id WHERE p.categorie_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapResultSetToProduit(rs));
                }
            }
        }
        return produits;
    }

    /**
     * Récupère toutes les catégories
     */
    public List<Categorie> findAllCategories() throws SQLException {
        List<Categorie> categories = new ArrayList<>();
        String sql = "SELECT * FROM categorie";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(new Categorie(rs.getInt("id"), rs.getString("nom")));
            }
        }
        return categories;
    }

    /**
     * Récupère un produit par son ID
     */
    public Produit findById(int id) throws SQLException {
        String sql = "SELECT p.*, c.nom as cat_nom FROM produit p LEFT JOIN categorie c ON p.categorie_id = c.id WHERE p.id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
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
    /**
     * Mappe un ResultSet vers un objet Produit
     */
    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit produit = new Produit();
        produit.setId(rs.getInt("id"));
        produit.setNom(rs.getString("nom"));
        produit.setDescription(rs.getString("description"));
        produit.setPrix(rs.getDouble("prix"));
        produit.setStock(rs.getInt("stock"));
        produit.setImageUrl(rs.getString("image_url"));
        
        int catId = rs.getInt("categorie_id");
        String catNom = rs.getString("cat_nom");
        if (catNom != null) {
            produit.setCategorie(new Categorie(catId, catNom));
        }
        
        return produit;
    }
}

