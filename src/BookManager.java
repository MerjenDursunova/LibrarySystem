import java.sql.*;
import java.util.Scanner;

public class BookManager {
    
    public static void browseAvailableBooks(Connection conn) {
        try {
            String sql = """
                SELECT b.ISBN, b.Title, b.Author, c.Name AS Category, 
                       p.Name AS Publisher, b.Available_Copies
                FROM Book b
                JOIN Category c ON b.CategoryID = c.CategoryID
                JOIN Publisher p ON b.PublisherID = p.PublisherID
                WHERE b.Available_Copies > 0
                ORDER BY b.Title
                """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            System.out.println("\n📖 Available Books:");
            System.out.println("ISBN | Title | Author | Category | Publisher | Available Copies");
            System.out.println("----------------------------------------------------------------");
            
            boolean hasBooks = false;
            while (rs.next()) {
                hasBooks = true;
                System.out.printf("%s | %s | %s | %s | %s | %d\n",
                    rs.getString("ISBN"),
                    rs.getString("Title"),
                    rs.getString("Author"),
                    rs.getString("Category"),
                    rs.getString("Publisher"),
                    rs.getInt("Available_Copies")
                );
            }
            
            if (!hasBooks) {
                System.out.println("No books available at the moment.");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    public static void viewAllBooks(Connection conn) {
        try {
            String sql = """
                SELECT b.ISBN, b.Title, b.Author, b.Year, c.Name AS Category, 
                       p.Name AS Publisher, b.Total_Copies, b.Available_Copies
                FROM Book b
                JOIN Category c ON b.CategoryID = c.CategoryID
                JOIN Publisher p ON b.PublisherID = p.PublisherID
                ORDER BY b.Title
                """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            System.out.println("\n📚 All Books in Library:");
            System.out.println("ISBN | Title | Author | Year | Category | Publisher | Total | Available");
            System.out.println("------------------------------------------------------------------------");
            
            while (rs.next()) {
                System.out.printf("%s | %s | %s | %d | %s | %s | %d | %d\n",
                    rs.getString("ISBN"),
                    rs.getString("Title"),
                    rs.getString("Author"),
                    rs.getInt("Year"),
                    rs.getString("Category"),
                    rs.getString("Publisher"),
                    rs.getInt("Total_Copies"),
                    rs.getInt("Available_Copies")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    public static void addBook(Connection conn, Scanner scanner) {
        try {
            System.out.println("\n➕ Add New Book");
            System.out.print("ISBN: ");
            String isbn = scanner.nextLine();
            
            System.out.print("Title: ");
            String title = scanner.nextLine();
            
            System.out.print("Author: ");
            String author = scanner.nextLine();
            
            System.out.print("Year: ");
            int year = scanner.nextInt();
            scanner.nextLine();
            
            System.out.print("Publisher ID (1=Penguin, 2=Oxford, 3=Springer): ");
            int publisherId = scanner.nextInt();
            
            System.out.print("Category ID (1=CS, 2=Math, 3=Physics, 4=Fiction, 5=Engineering): ");
            int categoryId = scanner.nextInt();
            
            System.out.print("Total Copies: ");
            int totalCopies = scanner.nextInt();
            scanner.nextLine();
            
            String sql = """
                INSERT INTO Book (ISBN, Title, Author, Year, PublisherID, CategoryID, 
                                  Total_Copies, Available_Copies)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, isbn);
            pstmt.setString(2, title);
            pstmt.setString(3, author);
            pstmt.setInt(4, year);
            pstmt.setInt(5, publisherId);
            pstmt.setInt(6, categoryId);
            pstmt.setInt(7, totalCopies);
            pstmt.setInt(8, totalCopies); // Initially all copies available
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Book added successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    public static void updateBook(Connection conn, Scanner scanner) {
        try {
            System.out.print("Enter ISBN of book to update: ");
            String isbn = scanner.nextLine();
            
            System.out.println("What would you like to update?");
            System.out.println("1. Title");
            System.out.println("2. Author");
            System.out.println("3. Total Copies");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            String column = "";
            String newValue = "";
            
            switch (choice) {
                case 1:
                    column = "Title";
                    System.out.print("New Title: ");
                    newValue = scanner.nextLine();
                    break;
                case 2:
                    column = "Author";
                    System.out.print("New Author: ");
                    newValue = scanner.nextLine();
                    break;
                case 3:
                    column = "Total_Copies";
                    System.out.print("New Total Copies: ");
                    newValue = scanner.nextLine();
                    break;
                default:
                    System.out.println("Invalid choice!");
                    return;
            }
            
            String sql = "UPDATE Book SET " + column + " = ? WHERE ISBN = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            if (choice == 3) {
                pstmt.setInt(1, Integer.parseInt(newValue));
            } else {
                pstmt.setString(1, newValue);
            }
            pstmt.setString(2, isbn);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Book updated successfully!");
            } else {
                System.out.println("❌ Book not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format!");
        }
    }
    
    public static void removeBook(Connection conn, Scanner scanner) {
        try {
            System.out.print("Enter ISBN of book to remove: ");
            String isbn = scanner.nextLine();
            
            // Check if book is borrowed
            String checkSql = """
                SELECT COUNT(*) FROM BorrowRecord 
                WHERE ISBN = ? AND Status IN ('Borrowed', 'Overdue')
                """;
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, isbn);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("❌ Cannot remove book: it is currently borrowed!");
                return;
            }
            
            // Delete from Book table (cascades to BorrowRecord and Reservation)
            String deleteSql = "DELETE FROM Book WHERE ISBN = ?";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setString(1, isbn);
            
            int rows = deleteStmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Book removed successfully!");
            } else {
                System.out.println("❌ Book not found!");
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}