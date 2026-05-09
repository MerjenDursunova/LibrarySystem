
// TestConnection.java
import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/library_system",
                    "root",
                    "yourpassword" // Your MySQL password
            );
            System.out.println("✅ Connected to library_system database!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Book");
            if (rs.next()) {
                System.out.println("📚 Total books in database: " + rs.getInt(1));
            }
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        }
    }
}