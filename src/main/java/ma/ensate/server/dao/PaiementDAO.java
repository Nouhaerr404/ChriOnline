package ma.ensate.server.dao;

import ma.ensate.models.MethodePaiement;
import ma.ensate.models.Paiement;
import ma.ensate.models.StatutPaiement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class PaiementDAO {

    /**
     * Sauvegarde un paiement dans la base de données
     */
    public boolean sauvegarder(Paiement paiement) throws SQLException {
        String sql = "INSERT INTO paiement (id, commande_id, date_payment, methode_payment, statut_payment, prix_a_payer, card_last4) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, paiement.getId());
            stmt.setString(2, paiement.getCommandeId());
            stmt.setObject(3, paiement.getDatePayment());
            stmt.setString(4, paiement.getMethodePayment().name());
            stmt.setString(5, paiement.getStatutPayment().name());
            stmt.setDouble(6, paiement.getPrixAPayer());
            stmt.setString(7, paiement.getCardLast4());
            
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Récupère un paiement par l'ID de la commande
     */
    public Paiement findByCommandeId(String commandeId) throws SQLException {
        String sql = "SELECT * FROM paiement WHERE commande_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, commandeId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Paiement paiement = new Paiement();
                    paiement.setId(rs.getString("id"));
                    paiement.setCommandeId(rs.getString("commande_id"));
                    
                    java.sql.Timestamp timestamp = rs.getTimestamp("date_payment");
                    if (timestamp != null) {
                        paiement.setDatePayment(timestamp.toLocalDateTime());
                    }
                    
                    paiement.setMethodePayment(MethodePaiement.valueOf(rs.getString("methode_payment")));
                    paiement.setStatutPayment(StatutPaiement.valueOf(rs.getString("statut_payment")));
                    paiement.setPrixAPayer(rs.getDouble("prix_a_payer"));
                    paiement.setCardLast4(rs.getString("card_last4"));
                    
                    return paiement;
                }
            }
        }
        return null;
    }
}

