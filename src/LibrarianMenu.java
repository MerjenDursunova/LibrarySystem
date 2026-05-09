import java.sql.*;
import java.util.Scanner;

public class LibrarianMenu {
    private static Scanner scanner = new Scanner(System.in);

    public static void showMenu(Connection conn) {
        while (true) {
            System.out.println("\n=== LIBRARIAN MENU ===");
            System.out.println("1. Add New Book");
            System.out.println("2. Update Book Information");
            System.out.println("3. Remove Book");
            System.out.println("4. View All Books");
            System.out.println("5. View Borrowing History");
            System.out.println("6. View Overdue Books");
            System.out.println("7. Calculate Fine for Borrow");
            System.out.println("8. View Reservations");
            System.out.println("9. Generate Reports");
            System.out.println("10. Logout");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    BookManager.addBook(conn, scanner);
                    break;
                case 2:
                    BookManager.updateBook(conn, scanner);
                    break;
                case 3:
                    BookManager.removeBook(conn, scanner);
                    break;
                case 4:
                    BookManager.viewAllBooks(conn);
                    break;
                case 5:
                    viewBorrowingHistory(conn);
                    break;
                case 6:
                    viewOverdueBooks(conn);
                    break;
                case 7:
                    calculateFine(conn);
                    break;
                case 8:
                    viewAllReservations(conn);
                    break;
                case 9:
                    generateReports(conn);
                    break;
                case 10:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private static void viewBorrowingHistory(Connection conn) {
        try {
            String sql = """
                    SELECT br.BorrowID, s.Name AS Student, b.Title, br.BorrowDate, br.DueDate,
                           br.ReturnDate, br.Status, br.FineAmount
                    FROM BorrowRecord br
                    JOIN Student s ON br.StudentID = s.StudentID
                    JOIN Book b ON br.ISBN = b.ISBN
                    ORDER BY br.BorrowDate DESC
                    LIMIT 50
                    """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n📊 Borrowing History (Last 50 records):");
            System.out.println("ID | Student | Title | Borrow Date | Due Date | Return Date | Status | Fine");
            System.out.println("----------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %s | %s | %s | $%.2f\n",
                        rs.getInt("BorrowID"),
                        rs.getString("Student"),
                        rs.getString("Title"),
                        rs.getDate("BorrowDate"),
                        rs.getDate("DueDate"),
                        rs.getDate("ReturnDate") != null ? rs.getDate("ReturnDate") : "N/A",
                        rs.getString("Status"),
                        rs.getDouble("FineAmount"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewOverdueBooks(Connection conn) {
        try {
            String sql = "SELECT * FROM OverdueBooks";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n⚠️  Overdue Books:");
            System.out.println("ID | Student | Email | Book Title | Borrow Date | Due Date | Days Overdue | Fine");
            System.out.println("-------------------------------------------------------------------------------");

            boolean hasOverdue = false;
            while (rs.next()) {
                hasOverdue = true;
                System.out.printf("%d | %s | %s | %s | %s | %s | %d | $%.2f\n",
                        rs.getInt("BorrowID"),
                        rs.getString("StudentName"),
                        rs.getString("Email"),
                        rs.getString("BookTitle"),
                        rs.getDate("BorrowDate"),
                        rs.getDate("DueDate"),
                        rs.getInt("DaysOverdue"),
                        rs.getDouble("FineAmount"));
            }

            if (!hasOverdue) {
                System.out.println("No overdue books! Great job!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void calculateFine(Connection conn) {
        try {
            System.out.print("Enter Borrow ID to calculate fine: ");
            int borrowId = scanner.nextInt();

            String sql = "CALL CalculateFine(?)";
            CallableStatement cstmt = conn.prepareCall(sql);
            cstmt.setInt(1, borrowId);

            ResultSet rs = cstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Result: " + rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewAllReservations(Connection conn) {
        try {
            String sql = """
                    SELECT r.ReservationID, s.Name AS Student, b.Title, r.ReserveDate, r.Status
                    FROM Reservation r
                    JOIN Student s ON r.StudentID = s.StudentID
                    JOIN Book b ON r.ISBN = b.ISBN
                    ORDER BY r.ReserveDate
                    """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n📋 All Active Reservations:");
            System.out.println("ID | Student | Book Title | Reserve Date | Status");
            System.out.println("---------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %s\n",
                        rs.getInt("ReservationID"),
                        rs.getString("Student"),
                        rs.getString("Title"),
                        rs.getDate("ReserveDate"),
                        rs.getString("Status"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void generateReports(Connection conn) {
        try {
            System.out.println("\n📈 Generate Reports:");
            System.out.println("1. Most Popular Books");
            System.out.println("2. Category Statistics");
            System.out.println("3. Student Borrowing Activity");
            System.out.print("Choose report: ");
            int reportChoice = scanner.nextInt();

            switch (reportChoice) {
                case 1:
                    // Most popular books
                    String sql1 = """
                            SELECT b.Title, b.Author, COUNT(br.BorrowID) AS BorrowCount
                            FROM Book b
                            LEFT JOIN BorrowRecord br ON b.ISBN = br.ISBN
                            GROUP BY b.ISBN
                            ORDER BY BorrowCount DESC
                            LIMIT 10
                            """;
                    executeAndPrintReport(conn, sql1, "Most Popular Books");
                    break;

                case 2:
                    // Category statistics
                    String sql2 = """
                            SELECT c.Name AS Category, COUNT(b.ISBN) AS BookCount,
                                   SUM(b.Total_Copies) AS TotalCopies,
                                   SUM(b.Available_Copies) AS AvailableCopies
                            FROM Category c
                            LEFT JOIN Book b ON c.CategoryID = b.CategoryID
                            GROUP BY c.CategoryID
                            """;
                    executeAndPrintReport(conn, sql2, "Category Statistics");
                    break;

                case 3:
                    // Student activity
                    String sql3 = """
                            SELECT s.StudentID, s.Name, COUNT(br.BorrowID) AS TotalBorrows,
                                   SUM(CASE WHEN br.Status = 'Overdue' THEN 1 ELSE 0 END) AS OverdueCount
                            FROM Student s
                            LEFT JOIN BorrowRecord br ON s.StudentID = br.StudentID
                            GROUP BY s.StudentID
                            ORDER BY TotalBorrows DESC
                            """;
                    executeAndPrintReport(conn, sql3, "Student Borrowing Activity");
                    break;

                default:
                    System.out.println("Invalid report choice!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void executeAndPrintReport(Connection conn, String sql, String title) throws SQLException {
        System.out.println("\n=== " + title + " ===");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        // Print headers
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(meta.getColumnName(i) + " | ");
        }
        System.out.println("\n" + "-".repeat(columnCount * 20));

        // Print data
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rs.getString(i) + " | ");
            }
            System.out.println();
        }
    }
}