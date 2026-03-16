package ma.ensate.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ma.ensate.util.ConfigLoader;

public class DBConnection {

    private static final String URL      = ConfigLoader.get("DB_URL", "jdbc:mysql://localhost:3306/chrionline");
    private static final String USER     = ConfigLoader.get("DB_USER", "root");
    private static final String PASSWORD = ConfigLoader.get("DB_PASSWORD", "");

    private static Connection instance = null;

    // Singleton — une seule connexion partagée
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connexion MySQL reussie !");
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
                System.out.println("Base de données connectee !");
            }
        } catch (SQLException e) {
            System.out.println(" Erreur : " + e.getMessage());
        }
    }
}