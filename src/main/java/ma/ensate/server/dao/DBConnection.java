package ma.ensate.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ma.ensate.util.ConfigLoader;

public class DBConnection {

    private static final String URL      = ConfigLoader.get(
            "DB_URL",
            "jdbc:mysql://127.0.0.1:8889/chrionline"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC"
                    + "&connectTimeout=5000"
                    + "&socketTimeout=5000"
                    + "&tcpKeepAlive=true");
    private static final String USER     = ConfigLoader.get("DB_USER", "root");
    private static final String PASSWORD = ConfigLoader.get("DB_PASSWORD", "");

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("Driver MySQL introuvable !");
            throw new SQLException(e);
        } catch (SQLException e) {
            System.err.println("[DBConnection] URL = " + URL);
            System.err.println("[DBConnection] SQLState = " + e.getSQLState());
            System.err.println("[DBConnection] ErrorCode = " + e.getErrorCode());
            if (e.getCause() != null) {
                System.err.println("[DBConnection] Cause = " + e.getCause().getClass().getName()
                        + " : " + e.getCause().getMessage());
            }
            throw e;
        }
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Base de données connectee !");
            }
        } catch (SQLException e) {
            System.out.println(" Erreur : " + e.getMessage());
        }
    }
}
