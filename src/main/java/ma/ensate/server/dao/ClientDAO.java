package ma.ensate.server.dao;

import ma.ensate.models.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientDAO {

    /**
     * Récupère un client par son ID
     * Jointure entre utilisateur et client car adresse et tel sont dans la table client
     */
    public Client findById(int id) throws SQLException {
        String sql = "SELECT u.id, u.nom, u.email, u.password, u.type_compte, u.session_token, " +
                     "c.adresse, c.tel " +
                     "FROM utilisateur u " +
                     "JOIN client c ON u.id = c.id " +
                     "WHERE u.id = ? AND u.type_compte = 'CLIENT'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Client client = new Client();
                    client.setId(rs.getInt("id"));
                    client.setNom(rs.getString("nom"));
                    client.setEmail(rs.getString("email"));
                    client.setPassword(rs.getString("password"));
                    client.setTypeCompte(rs.getString("type_compte"));
                    client.setSessionToken(rs.getString("session_token"));
                    client.setAdresse(rs.getString("adresse"));
                    client.setTel(rs.getString("tel"));
                    return client;
                }
            }
        }
        return null;
    }
}

