package ma.ensate.server.dao;

import ma.ensate.models.Client;
import ma.ensate.models.Utilisateur;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UtilisateurDAO {

    // Logger Log4j
    private static final Logger logger = LogManager.getLogger(UtilisateurDAO.class);

    // email → nombre de tentatives échouées
    private static final Map<String, Integer> tentatives = new HashMap<>();
    // email → timestamp du blocage
    private static final Map<String, Long> blocages = new HashMap<>();
    // Constantes
    private static final int MAX_TENTATIVES  = 3;
    private static final int DUREE_BLOCAGE_MS = 5 * 60 * 1000; // 5 minutes


    public static String hasherMotDePasse(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Erreur hashage SHA-256 : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean estBloque(String email) {
        if (!blocages.containsKey(email)) return false;

        long tempsBlocage = blocages.get(email);
        long maintenant   = System.currentTimeMillis();

        if (maintenant - tempsBlocage < DUREE_BLOCAGE_MS) {
            long resteMs      = DUREE_BLOCAGE_MS - (maintenant - tempsBlocage);
            long resteMinutes = resteMs / 60000;
            logger.warn("Compte bloque : " + email +
                    " | Reste : " + resteMinutes + " minutes");
            return true;
        } else {
            // Blocage expiré → réinitialiser
            blocages.remove(email);
            tentatives.remove(email);
            return false;
        }
    }


    public void enregistrerEchec(String email) {
        int nb = tentatives.getOrDefault(email, 0) + 1;
        tentatives.put(email, nb);

        logger.warn("Echec login pour : " + email +
                " | Tentative " + nb + "/" + MAX_TENTATIVES);

        if (nb >= MAX_TENTATIVES) {
            blocages.put(email, System.currentTimeMillis());
            tentatives.remove(email);
            logger.warn(" Compte bloque 5 minutes : " + email);
        }
    }


    public void reinitialiserTentatives(String email) {
        if (estBloque(email)) {
            return;
        }
        tentatives.remove(email);
        blocages.remove(email);
    }

    public boolean emailExiste(String email) throws SQLException {
        // PreparedStatement → protection SQL Injection (TP7)
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public boolean inscrire(Client client) throws SQLException {
        // 1. Insérer dans la table utilisateur
        String sqlUser = "INSERT INTO utilisateur (nom, email, password, type_compte) " +
                "VALUES (?, ?, ?, 'CLIENT')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUser,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, client.getNom());
            ps.setString(2, client.getEmail());
            ps.setString(3, hasherMotDePasse(client.getPassword()));
            ps.executeUpdate();


            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int idGenere = keys.getInt(1);
                client.setId(idGenere);

                // 2. Insérer dans la table client
                String sqlClient = "INSERT INTO client (id, adresse, tel) VALUES (?, ?, ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(sqlClient)) {
                    ps2.setInt(1, idGenere);
                    ps2.setString(2, client.getAdresse());
                    ps2.setString(3, client.getTel());
                    ps2.executeUpdate();
                }

                logger.info(" Nouvel utilisateur inscrit : " + client.getEmail());
                return true;
            }
        }
        return false;
    }

    public Utilisateur trouverParEmailPassword(String email, String password)
            throws SQLException {

        String passwordHashe = hasherMotDePasse(password);
        String sql = "SELECT u.*, c.adresse, c.tel " +
                "FROM utilisateur u " +
                "LEFT JOIN client c ON c.id = u.id " +
                "WHERE u.email = ? AND u.password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, passwordHashe);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String type = rs.getString("type_compte");
                Utilisateur u;

                if ("CLIENT".equals(type)) {
                    Client c = new Client();
                    c.setAdresse(rs.getString("adresse"));
                    c.setTel(rs.getString("tel"));
                    u = c;
                } else {
                    u = new Utilisateur();
                }

                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setEmail(rs.getString("email"));
                u.setTypeCompte(type);
                return u;
            }
        }
        return null; // pas trouvé
    }

    public void sauvegarderToken(int userId, String token) throws SQLException {
        String sql = "UPDATE utilisateur SET session_token = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
            logger.info("Token sauvegardé pour userId : " + userId);
        }
    }


    public void supprimerToken(int userId) throws SQLException {
        String sql = "UPDATE utilisateur SET session_token = NULL WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            logger.info("Token supprimé pour userId : " + userId);
        }
    }


    public Utilisateur trouverParToken(String token) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE session_token = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Utilisateur u = new Utilisateur();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setEmail(rs.getString("email"));
                u.setTypeCompte(rs.getString("type_compte"));
                u.setSessionToken(token);
                return u;
            }
        }
        return null;
    }
}