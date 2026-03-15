package ma.ensate.server.services;

import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;
import ma.ensate.protocol.Response;
import ma.ensate.server.dao.ProduitDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private static final Logger logger = LogManager.getLogger(ProductService.class);
    private final ProduitDAO produitDAO;

    public ProductService() {
        this.produitDAO = new ProduitDAO();
    }

    public Response getAllProducts() {
        try {
            List<Produit> produits = produitDAO.findAll();
            return new Response(true, "Produits récupérés avec succès", produits);
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des produits : " + e.getMessage());
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    public Response getProductById(Object data) {
        try {
            if (!(data instanceof Integer)) {
                return new Response(false, "ID produit invalide");
            }
            int id = (Integer) data;
            Produit produit = produitDAO.findById(id);
            if (produit != null) {
                return new Response(true, "Produit trouvé", produit);
            } else {
                return new Response(false, "Produit introuvable");
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération du produit : " + e.getMessage());
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    public Response getProductsByCategory(Object data) {
        try {
            if (!(data instanceof Integer)) {
                return new Response(false, "ID catégorie invalide");
            }
            int catId = (Integer) data;
            List<Produit> produits = produitDAO.findByCategorie(catId);
            return new Response(true, "Produits de la catégorie récupérés", produits);
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des produits par catégorie : " + e.getMessage());
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }

    public Response getAllCategories() {
        try {
            List<Categorie> categories = produitDAO.findAllCategories();
            return new Response(true, "Catégories récupérées avec succès", categories);
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des catégories : " + e.getMessage());
            return new Response(false, "Erreur base de données : " + e.getMessage());
        }
    }
}
