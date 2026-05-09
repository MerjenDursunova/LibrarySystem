import java.sql.*;
import java.util.Scanner;

public class LibraryApp {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("📚 Welcome to Library Management System 📚");

        while (true) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. Student Login");
            System.out.println("2. Librarian Login");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    studentLogin();
                    break;
                case 2:
                    librarianLogin();
                    break;
                case 3:
                    System.out.println("Goodbye!");
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static void studentLogin() {
        System.out.print("\nEnter Student ID: ");
        int studentId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM Student WHERE StudentID = ? AND Email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            pstmt.setString(2, email);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n✅ Login successful! Welcome, " + rs.getString("Name"));
                StudentMenu.showMenu(studentId, conn);
            } else {
                System.out.println("❌ Invalid credentials!");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static void librarianLogin() {
        System.out.print("\nEnter Email: ");
        String email = scanner.nextLine();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM Librarian WHERE Email = ? AND Password = SHA2(?, 256)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n✅ Login successful! Welcome, " + rs.getString("Name"));
                LibrarianMenu.showMenu(conn);
            } else {
                System.out.println("❌ Invalid credentials!");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}