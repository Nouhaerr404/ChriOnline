package ma.ensate.server.dao;

import ma.ensate.models.Client;
import ma.ensate.models.Utilisateur;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilisateurDAO {

    private static final Logger logger = LogManager.getLogger(UtilisateurDAO.class);

    private static final Map<String, Integer> tentatives = new HashMap<>();

    private static final Map<String, Long> blocages = new HashMap<>();

    private static final int MAX_TENTATIVES  = 3;
    private static final int DUREE_BLOCAGE_MS = 5 * 60 * 1000; // 5 minutes
    private static final int BCRYPT_COST = 12;


    public static String hasherMotDePasse(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
    }

    private static String hasherMotDePasseLegacySHA256(String password) {
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

    private boolean verifierMotDePasse(String passwordEnClair, String hashStocke) {
        if (hashStocke == null || hashStocke.isBlank()) {
            return false;
        }

        if (hashStocke.startsWith("$2a$") || hashStocke.startsWith("$2b$") || hashStocke.startsWith("$2y$")) {
            return BCrypt.checkpw(passwordEnClair, hashStocke);
        }

        // Compatibilite temporaire avec les anciens comptes SHA-256.
        return hashStocke.equals(hasherMotDePasseLegacySHA256(passwordEnClair));
    }

    private void mettreAJourHashMotDePasse(int userId, String nouveauHash) throws SQLException {
        String sqlUpdate = "UPDATE utilisateur SET password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setString(1, nouveauHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
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

        String sql = "SELECT u.*, c.adresse, c.tel " +
                "FROM utilisateur u " +
                "LEFT JOIN client c ON c.id = u.id " +
                "WHERE u.email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String hashStocke = rs.getString("password");
                if (!verifierMotDePasse(password, hashStocke)) {
                    return null;
                }

                // Lire TOUTES les colonnes avant tout appel DB
                // (sinon mettreAJourHashMotDePasse ferme le ResultSet)
                int    id      = rs.getInt("id");
                String type    = rs.getString("type_compte");
                String nom     = rs.getString("nom");
                String emailDb = rs.getString("email");
                String statut  = rs.getString("statut");
                boolean twoFa  = rs.getBoolean("two_fa_enabled");
                String adresse = rs.getString("adresse");
                String tel     = rs.getString("tel");

                if (!hashStocke.startsWith("$2")) {
                    mettreAJourHashMotDePasse(id, hasherMotDePasse(password));
                    logger.info("Migration SHA-256 -> BCrypt effectuee pour {}", email);
                }

                Utilisateur u;
                if ("CLIENT".equals(type)) {
                    Client c = new Client();
                    c.setAdresse(adresse);
                    c.setTel(tel);
                    u = c;
                } else {
                    u = new Utilisateur();
                }

                u.setId(id);
                u.setNom(nom);
                u.setEmail(emailDb);
                u.setTypeCompte(type);
                u.setStatut(statut);
                u.setTwoFaEnabled(twoFa);
                return u;
            }
        }
        return null;
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
                u.setTwoFaEnabled(rs.getBoolean("two_fa_enabled"));
                return u;
            }
        }
        return null;
    }

    public boolean setTwoFaEnabled(int userId, boolean enabled) throws SQLException {
        String sql = "UPDATE utilisateur SET two_fa_enabled = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            logger.info("2FA " + (enabled ? "activé" : "désactivé") + " pour userId : " + userId);
            return rows > 0;
        }
    }
    public List<Utilisateur> findAll() throws SQLException {
        String sql = """
            SELECT u.id, u.nom, u.email, u.type_compte, u.statut,
                   c.adresse, c.tel
            FROM utilisateur u
            JOIN client c ON c.id = u.id
            WHERE u.type_compte = 'CLIENT'
            ORDER BY u.id
            """;

        List<Utilisateur> liste = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Client c = new Client();
                c.setId(rs.getInt("id"));
                c.setNom(rs.getString("nom"));
                c.setEmail(rs.getString("email"));
                c.setTypeCompte(rs.getString("type_compte"));
                c.setStatut(rs.getString("statut"));
                c.setAdresse(rs.getString("adresse"));
                c.setTel(rs.getString("tel"));
                liste.add(c);
            }
        }
        return liste;
    }

    public Utilisateur findById(int id) throws SQLException {
        String sql = """
                SELECT u.id, u.nom, u.email, u.type_compte, u.statut, c.adresse, c.tel
                FROM utilisateur u
                LEFT JOIN client c ON c.id = u.id
                WHERE u.id = ?"
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type_compte");
                Utilisateur u ;
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
                u.setStatut(rs.getString("statut"));
                return u;
            }
        }
        return null;
    }
    public boolean suspendreCompte(int userId) throws SQLException {
        String sql = "Update utilisateur SET statut='SUSPENDU' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            logger.info("Compte suspendu : userId : " + userId);
            return rows > 0;
        }
    }
    public boolean reactiverCompte(int userId) throws SQLException {
        String sql = "UPDATE utilisateur SET statut = 'ACTIF' WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            logger.info("Compte réactivé : userId=" + userId);
            return rows > 0;
        }
    }

    public Utilisateur trouverParId(int id) throws SQLException {
        String sql = "SELECT u.*, c.adresse, c.tel " +
                "FROM utilisateur u " +
                "LEFT JOIN client c ON c.id = u.id " +
                "WHERE u.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Client client = new Client();
                client.setId(rs.getInt("id"));
                client.setNom(rs.getString("nom"));
                client.setEmail(rs.getString("email"));
                client.setTypeCompte(rs.getString("type_compte"));
                client.setAdresse(rs.getString("adresse"));
                client.setTel(rs.getString("tel"));
                client.setStatut(rs.getString("statut"));
                client.setTwoFaEnabled(rs.getBoolean("two_fa_enabled"));
                return client;
            }
        }
        return null;
    }

    public boolean mettreAJourProfil(int id, String nom,
                                     String adresse,
                                     String tel) throws SQLException {

        String sqlUser = "UPDATE utilisateur SET nom = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUser)) {
            ps.setString(1, nom);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        String sqlClient = "UPDATE client SET adresse = ?, tel = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlClient)) {
            ps.setString(1, adresse);
            ps.setString(2, tel);
            ps.setInt(3, id);
            ps.executeUpdate();
        }

        logger.info("Profil mis à jour pour userId : " + id);
        return true;
    }

    public boolean changerMotDePasse(int id, String ancienPassword,
                                     String nouveauPassword) throws SQLException {

        String sqlVerif = "SELECT password FROM utilisateur WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlVerif)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || !verifierMotDePasse(ancienPassword, rs.getString("password"))) {
                logger.warn("Ancien mot de passe incorrect pour userId : " + id);
                return false;
            }
        }

        // Mettre à jour avec le nouveau mot de passe hashé
        mettreAJourHashMotDePasse(id, hasherMotDePasse(nouveauPassword));
        logger.info("Mot de passe changé pour userId : " + id);
        return true;
    }
}