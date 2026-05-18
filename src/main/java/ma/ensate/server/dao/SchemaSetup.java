package ma.ensate.server.dao;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaSetup {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
             
            stmt.execute("CREATE TABLE IF NOT EXISTS security_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "ip_address VARCHAR(45) NOT NULL," +
                    "user_identifier VARCHAR(100)," +
                    "action_type VARCHAR(50) NOT NULL," +
                    "status VARCHAR(20) NOT NULL," +
                    "details TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS security_alerts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "alert_type VARCHAR(50) NOT NULL," +
                    "severity VARCHAR(20) NOT NULL," +
                    "target_ip VARCHAR(45)," +
                    "target_user_id INT," +
                    "description TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS blocked_ips (" +
                    "ip_address VARCHAR(45) PRIMARY KEY," +
                    "blocked_until DATETIME NOT NULL," +
                    "reason VARCHAR(255))");
                    
            System.out.println("Tables de securite creees avec succes.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
