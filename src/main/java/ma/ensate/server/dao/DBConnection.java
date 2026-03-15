package ma.ensate.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:8889/chrionline";
    private static final String USER     = "root";
    private static final String PASSWORD = "root";

    private static Connection instance = null;

    // Singleton — une seule connexion partagée
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connexion MySQL réussie !");
            } catch (ClassNotFoundException e) {
                System.out.println("Driver MySQL introuvable !");
                throw new SQLException(e);
            }
        }
        return instance;
    }

    // Tester la connexion
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                System.out.println("Base de données connectée !");
            }
        } catch (SQLException e) {
            System.out.println(" Erreur : " + e.getMessage());
        }
    }
}