package ma.ensate.server.dao;

import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
     * Ajoute un produit en base.
     */
    public Produit ajouter(Produit produit) throws SQLException {
        String sql = "INSERT INTO produit(nom, description, prix, stock, image_url, categorie_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setDouble(3, produit.getPrix());
            stmt.setInt(4, produit.getStock());
            stmt.setString(5, produit.getImageUrl());
            stmt.setInt(6, produit.getCategorie().getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                return null;
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    produit.setId(keys.getInt(1));
                }
            }
            return produit;
        }
    }

    /**
     * Modifie un produit existant en base.
     */
    public boolean modifier(Produit produit) throws SQLException {
        String sql = "UPDATE produit SET nom = ?, description = ?, prix = ?, stock = ?, image_url = ?, categorie_id = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setDouble(3, produit.getPrix());
            stmt.setInt(4, produit.getStock());
            stmt.setString(5, produit.getImageUrl());
            stmt.setInt(6, produit.getCategorie().getId());
            stmt.setInt(7, produit.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprime un produit par identifiant.
     */
    public boolean supprimer(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
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

