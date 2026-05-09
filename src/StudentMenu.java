import java.sql.*;
import java.util.Scanner;

public class StudentMenu {
    private static Scanner scanner = new Scanner(System.in);
    
    public static void showMenu(int studentId, Connection conn) {
        while (true) {
            System.out.println("\n=== STUDENT MENU ===");
            System.out.println("1. Browse Available Books");
            System.out.println("2. Borrow a Book");
            System.out.println("3. Return a Book");
            System.out.println("4. View My Borrowed Books");
            System.out.println("5. Reserve a Book");
            System.out.println("6. View My Reservations");
            System.out.println("7. Logout");
            System.out.print("Enter choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    BookManager.browseAvailableBooks(conn);
                    break;
                case 2:
                    BorrowManager.borrowBook(studentId, conn, scanner);
                    break;
                case 3:
                    BorrowManager.returnBook(studentId, conn, scanner);
                    break;
                case 4:
                    viewMyBorrowedBooks(studentId, conn);
                    break;
                case 5:
                    reserveBook(studentId, conn);
                    break;
                case 6:
                    viewMyReservations(studentId, conn);
                    break;
                case 7:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
    
    private static void viewMyBorrowedBooks(int studentId, Connection conn) {
        try {
            String sql = """
                SELECT br.BorrowID, b.Title, b.Author, br.BorrowDate, br.DueDate, br.Status, br.FineAmount
                FROM BorrowRecord br
                JOIN Book b ON br.ISBN = b.ISBN
                WHERE br.StudentID = ? AND br.Status IN ('Borrowed', 'Overdue')
                ORDER BY br.DueDate
                """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n📚 Your Borrowed Books:");
            System.out.println("ID | Title | Author | Borrow Date | Due Date | Status | Fine");
            System.out.println("----------------------------------------------------------------");
            
            boolean hasBooks = false;
            while (rs.next()) {
                hasBooks = true;
                System.out.printf("%d | %s | %s | %s | %s | %s | $%.2f\n",
                    rs.getInt("BorrowID"),
                    rs.getString("Title"),
                    rs.getString("Author"),
                    rs.getDate("BorrowDate"),
                    rs.getDate("DueDate"),
                    rs.getString("Status"),
                    rs.getDouble("FineAmount")
                );
            }
            
            if (!hasBooks) {
                System.out.println("No borrowed books found.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void reserveBook(int studentId, Connection conn) {
        try {
            System.out.print("Enter ISBN to reserve: ");
            String isbn = scanner.nextLine();
            
            // Check if book exists and is unavailable
            String checkSql = "SELECT Available_Copies FROM Book WHERE ISBN = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, isbn);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                if (rs.getInt("Available_Copies") > 0) {
                    System.out.println("❌ Book is available for borrowing. No need to reserve.");
                    return;
                }
                
                // Insert reservation
                String sql = "INSERT INTO Reservation (StudentID, ISBN, ReserveDate) VALUES (?, ?, CURDATE())";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, studentId);
                pstmt.setString(2, isbn);
                
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Book reserved successfully!");
                }
            } else {
                System.out.println("❌ Book not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void viewMyReservations(int studentId, Connection conn) {
        try {
            String sql = """
                SELECT r.ReservationID, b.Title, b.Author, r.ReserveDate, r.Status
                FROM Reservation r
                JOIN Book b ON r.ISBN = b.ISBN
                WHERE r.StudentID = ?
                ORDER BY r.ReserveDate DESC
                """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n📋 Your Reservations:");
            System.out.println("ID | Title | Author | Reserve Date | Status");
            System.out.println("------------------------------------------------");
            
            boolean hasReservations = false;
            while (rs.next()) {
                hasReservations = true;
                System.out.printf("%d | %s | %s | %s | %s\n",
                    rs.getInt("ReservationID"),
                    rs.getString("Title"),
                    rs.getString("Author"),
                    rs.getDate("ReserveDate"),
                    rs.getString("Status")
                );
            }
            
            if (!hasReservations) {
                System.out.println("No reservations found.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}