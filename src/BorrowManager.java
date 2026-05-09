import java.sql.*;
import java.util.Scanner;

public class BorrowManager {

    public static void borrowBook(int studentId, Connection conn, Scanner scanner) {
        try {
            // Show available books first
            BookManager.browseAvailableBooks(conn);

            System.out.print("\nEnter ISBN to borrow: ");
            String isbn = scanner.nextLine();

            // Check if book is available
            String checkSql = "SELECT Available_Copies FROM Book WHERE ISBN = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, isbn);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int available = rs.getInt("Available_Copies");
                if (available <= 0) {
                    System.out.println("❌ Book is not available for borrowing!");
                    return;
                }

                // Check if student already borrowed this book and hasn't returned
                String checkBorrowSql = """
                        SELECT COUNT(*) FROM BorrowRecord
                        WHERE StudentID = ? AND ISBN = ? AND Status IN ('Borrowed', 'Overdue')
                        """;
                PreparedStatement checkBorrowStmt = conn.prepareStatement(checkBorrowSql);
                checkBorrowStmt.setInt(1, studentId);
                checkBorrowStmt.setString(2, isbn);
                ResultSet rs2 = checkBorrowStmt.executeQuery();

                if (rs2.next() && rs2.getInt(1) > 0) {
                    System.out.println("❌ You already have this book borrowed!");
                    return;
                }

                // Insert borrow record (trigger will update Available_Copies)
                String sql = """
                        INSERT INTO BorrowRecord (StudentID, ISBN, BorrowDate, DueDate, Status)
                        VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Borrowed')
                        """;
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, studentId);
                pstmt.setString(2, isbn);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Book borrowed successfully!");
                    System.out.println("Due Date: " + java.time.LocalDate.now().plusDays(14));

                    // Update any active reservation for this book by this student
                    String updateReservationSql = """
                            UPDATE Reservation
                            SET Status = 'Fulfilled'
                            WHERE StudentID = ? AND ISBN = ? AND Status = 'Active'
                            """;
                    PreparedStatement updateStmt = conn.prepareStatement(updateReservationSql);
                    updateStmt.setInt(1, studentId);
                    updateStmt.setString(2, isbn);
                    updateStmt.executeUpdate();
                }
            } else {
                System.out.println("❌ Book not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void returnBook(int studentId, Connection conn, Scanner scanner) {
        try {
            // Show student's borrowed books
            String borrowedSql = """
                    SELECT br.BorrowID, b.Title, b.Author, br.BorrowDate, br.DueDate
                    FROM BorrowRecord br
                    JOIN Book b ON br.ISBN = b.ISBN
                    WHERE br.StudentID = ? AND br.Status IN ('Borrowed', 'Overdue')
                    """;
            PreparedStatement pstmt = conn.prepareStatement(borrowedSql);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n📚 Your Borrowed Books:");
            System.out.println("BorrowID | Title | Author | Borrow Date | Due Date");
            System.out.println("---------------------------------------------------");

            boolean hasBooks = false;
            while (rs.next()) {
                hasBooks = true;
                System.out.printf("%d | %s | %s | %s | %s\n",
                        rs.getInt("BorrowID"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getDate("BorrowDate"),
                        rs.getDate("DueDate"));
            }

            if (!hasBooks) {
                System.out.println("No borrowed books to return.");
                return;
            }

            System.out.print("\nEnter Borrow ID to return: ");
            int borrowId = scanner.nextInt();
            scanner.nextLine();

            // Calculate fine if overdue
            String checkFineSql = """
                    SELECT DATEDIFF(CURDATE(), DueDate) AS DaysOverdue
                    FROM BorrowRecord
                    WHERE BorrowID = ? AND Status IN ('Borrowed', 'Overdue')
                    """;
            PreparedStatement checkFineStmt = conn.prepareStatement(checkFineSql);
            checkFineStmt.setInt(1, borrowId);
            ResultSet rs2 = checkFineStmt.executeQuery();

            double fine = 0.0;
            if (rs2.next()) {
                int daysOverdue = rs2.getInt("DaysOverdue");
                if (daysOverdue > 0) {
                    fine = daysOverdue * 0.50; // $0.50 per day
                    System.out.printf("⚠️  Book is %d days overdue. Fine: $%.2f\n", daysOverdue, fine);
                }
            }

            // Update borrow record (trigger will update Available_Copies)
            String returnSql = """
                    UPDATE BorrowRecord
                    SET ReturnDate = CURDATE(), Status = 'Returned', FineAmount = ?
                    WHERE BorrowID = ? AND StudentID = ?
                    """;
            PreparedStatement returnStmt = conn.prepareStatement(returnSql);
            returnStmt.setDouble(1, fine);
            returnStmt.setInt(2, borrowId);
            returnStmt.setInt(3, studentId);

            int rows = returnStmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Book returned successfully!");
                if (fine > 0) {
                    System.out.printf("Please pay fine: $%.2f\n", fine);
                }
            } else {
                System.out.println("❌ Return failed! Check Borrow ID.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}