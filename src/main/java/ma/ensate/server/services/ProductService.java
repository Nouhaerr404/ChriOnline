package ma.ensate.server.services;

import ma.ensate.models.Categorie;
import ma.ensate.models.Produit;
import ma.ensate.protocol.Response;
import ma.ensate.protocol.dto.ProduitAdminRequest;
import ma.ensate.server.dao.ProduitDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ProductService {
    private static final Logger logger = LogManager.getLogger(ProductService.class);
    private static final Path IMAGE_UPLOAD_DIR = Paths.get("uploads", "produits");
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

    public Response createProduct(Object data) {
        try {
            if (!(data instanceof ProduitAdminRequest req)) {
                return new Response(false, "Données produit invalides");
            }

            Response validation = validateRequest(req, false);
            if (!validation.isSuccess()) {
                return validation;
            }

            Produit produit = mapRequestToProduit(req);
            if (hasImagePayload(req)) {
                produit.setImageUrl(saveImage(req.getImageBytes(), req.getImageFileName()));
            }

            Produit created = produitDAO.ajouter(produit);
            if (created == null) {
                return new Response(false, "Echec de creation du produit");
            }
            return new Response(true, "Produit cree avec succes", created);
        } catch (SQLException e) {
            logger.error("Erreur lors de la creation du produit : " + e.getMessage());
            return new Response(false, "Erreur base de donnees : " + e.getMessage());
        } catch (IOException e) {
            logger.error("Erreur lors de l'enregistrement de l'image : " + e.getMessage());
            return new Response(false, "Erreur enregistrement image : " + e.getMessage());
        }
    }

    public Response updateProduct(Object data) {
        try {
            if (!(data instanceof ProduitAdminRequest req)) {
                return new Response(false, "Données produit invalides");
            }

            Response validation = validateRequest(req, true);
            if (!validation.isSuccess()) {
                return validation;
            }

            Produit existing = produitDAO.findById(req.getId());
            if (existing == null) {
                return new Response(false, "Produit introuvable");
            }

            Produit produit = mapRequestToProduit(req);
            produit.setId(req.getId());

            if (hasImagePayload(req)) {
                produit.setImageUrl(saveImage(req.getImageBytes(), req.getImageFileName()));
            } else if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
                produit.setImageUrl(req.getImageUrl());
            } else {
                produit.setImageUrl(existing.getImageUrl());
            }

            boolean updated = produitDAO.modifier(produit);
            if (!updated) {
                return new Response(false, "Echec de mise a jour du produit");
            }

            Produit refreshed = produitDAO.findById(req.getId());
            return new Response(true, "Produit modifie avec succes", refreshed != null ? refreshed : produit);
        } catch (SQLException e) {
            logger.error("Erreur lors de la modification du produit : " + e.getMessage());
            return new Response(false, "Erreur base de donnees : " + e.getMessage());
        } catch (IOException e) {
            logger.error("Erreur lors de l'enregistrement de l'image : " + e.getMessage());
            return new Response(false, "Erreur enregistrement image : " + e.getMessage());
        }
    }

    public Response deleteProduct(Object data) {
        try {
            if (!(data instanceof Integer id) || id <= 0) {
                return new Response(false, "ID produit invalide");
            }

            boolean deleted = produitDAO.supprimer(id);
            if (!deleted) {
                return new Response(false, "Produit introuvable ou deja supprime");
            }
            return new Response(true, "Produit supprime avec succes");
        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du produit : " + e.getMessage());
            return new Response(false, "Erreur base de donnees : " + e.getMessage());
        }
    }

    private Response validateRequest(ProduitAdminRequest req, boolean isUpdate) {
        if (isUpdate && (req.getId() == null || req.getId() <= 0)) {
            return new Response(false, "ID produit invalide");
        }
        if (req.getNom() == null || req.getNom().isBlank()) {
            return new Response(false, "Nom produit obligatoire");
        }
        if (req.getPrix() == null || req.getPrix() < 0) {
            return new Response(false, "Prix invalide");
        }
        if (req.getStock() == null || req.getStock() < 0) {
            return new Response(false, "Stock invalide");
        }
        if (req.getCategorieId() == null || req.getCategorieId() <= 0) {
            return new Response(false, "Categorie invalide");
        }
        return new Response(true, "OK");
    }

    private Produit mapRequestToProduit(ProduitAdminRequest req) {
        Produit produit = new Produit();
        produit.setNom(req.getNom().trim());
        produit.setDescription(req.getDescription() == null ? "" : req.getDescription().trim());
        produit.setPrix(req.getPrix());
        produit.setStock(req.getStock());
        produit.setCategorie(new Categorie(req.getCategorieId(), null));
        return produit;
    }

    private boolean hasImagePayload(ProduitAdminRequest req) {
        return req.getImageBytes() != null && req.getImageBytes().length > 0;
    }

    private String saveImage(byte[] bytes, String originalName) throws IOException {
        Files.createDirectories(IMAGE_UPLOAD_DIR);

        String extension = ".bin";
        if (originalName != null && originalName.contains(".")) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                extension = originalName.substring(dot);
            }
        }

        String fileName = "product_" + UUID.randomUUID() + extension;
        Path imagePath = IMAGE_UPLOAD_DIR.resolve(fileName);
        Files.write(imagePath, bytes);
        return imagePath.toAbsolutePath().toUri().toString();
    }
}
