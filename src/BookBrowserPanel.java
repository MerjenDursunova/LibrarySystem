import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class BookBrowserPanel extends JPanel {
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private int studentId;
    private boolean isLibrarian;

    public BookBrowserPanel(int studentId, boolean isLibrarian) {
        this.studentId = studentId;
        this.isLibrarian = isLibrarian;

        setLayout(new BorderLayout());

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(30);
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        // Table (include Category and Publisher columns to match loaded data)
        String[] columns = { "ISBN", "Title", "Author", "Year", "Available", "Total", "Category", "Publisher" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bookTable = new JTable(tableModel);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(bookTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton detailsButton = new JButton("View Details");
        JButton actionButton = isLibrarian ? new JButton("Delete Book") : new JButton("Borrow Book");
        JButton reserveButton = new JButton("Reserve");

        buttonPanel.add(detailsButton);
        buttonPanel.add(actionButton);
        if (!isLibrarian) {
            buttonPanel.add(reserveButton);
        }

        // Add components
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load data
        loadBooks("");

        // Event listeners
        searchButton.addActionListener(e -> loadBooks(searchField.getText()));
        refreshButton.addActionListener(e -> loadBooks(""));
        searchField.addActionListener(e -> loadBooks(searchField.getText()));

        detailsButton.addActionListener(e -> showBookDetails());
        actionButton.addActionListener(e -> {
            if (isLibrarian)
                deleteBook();
            else
                borrowBook();
        });
        reserveButton.addActionListener(e -> reserveBook());
    }

    private void loadBooks(String searchTerm) {
        tableModel.setRowCount(0); // Clear table

        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT b.ISBN, b.Title, b.Author, b.Year, b.Available_Copies, b.Total_Copies,
                           c.Name AS Category, p.Name AS Publisher
                    FROM Book b
                    JOIN Category c ON b.CategoryID = c.CategoryID
                    JOIN Publisher p ON b.PublisherID = p.PublisherID
                    WHERE b.Title LIKE ? OR b.Author LIKE ? OR b.ISBN LIKE ?
                    ORDER BY b.Title
                    """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            String likeTerm = "%" + searchTerm + "%";
            pstmt.setString(1, likeTerm);
            pstmt.setString(2, likeTerm);
            pstmt.setString(3, likeTerm);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Object[] row = {
                        rs.getString("ISBN"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getInt("Year"),
                        rs.getInt("Available_Copies"),
                        rs.getInt("Total_Copies"),
                        rs.getString("Category"),
                        rs.getString("Publisher")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading books: " + e.getMessage());
        }
    }

    private void showBookDetails() {
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book first!");
            return;
        }

        String isbn = (String) tableModel.getValueAt(row, 0);

        Frame top = (Frame) SwingUtilities.getWindowAncestor(this);
        new BookDetailsDialog(top, isbn, true);

    }

    private void borrowBook() {
        if (studentId == 0) {
            JOptionPane.showMessageDialog(this, "No student logged in!");
            return;
        }

        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book first!");
            return;
        }

        String isbn = (String) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);
        int available = (int) tableModel.getValueAt(row, 4);

        if (available <= 0) {
            JOptionPane.showMessageDialog(this, "This book is not available for borrowing!");
            return;
        }

        // Check if student already borrowed this book
        try (Connection conn = DBUtil.getConnection()) {
            String checkSql = """
                    SELECT COUNT(*) FROM BorrowRecord
                    WHERE StudentID = ? AND ISBN = ? AND Status IN ('Borrowed', 'Overdue')
                    """;
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, studentId);
            checkStmt.setString(2, isbn);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "You already have this book borrowed!");
                return;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error checking existing borrows: " + e.getMessage());
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Borrow '" + title + "'?\nDue in 14 days.",
                "Confirm Borrow", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBUtil.getConnection()) {
                // START TRANSACTION
                conn.setAutoCommit(false);

                try {
                    // 1. Insert borrow record (trigger will update Available_Copies)
                    String sql = """
                            INSERT INTO BorrowRecord (StudentID, ISBN, BorrowDate, DueDate, Status)
                            VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Borrowed')
                            """;
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, studentId);
                    pstmt.setString(2, isbn);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        // 2. Update any active reservation
                        String updateReservationSql = """
                                UPDATE Reservation
                                SET Status = 'Fulfilled'
                                WHERE StudentID = ? AND ISBN = ? AND Status = 'Active'
                                """;
                        PreparedStatement updateStmt = conn.prepareStatement(updateReservationSql);
                        updateStmt.setInt(1, studentId);
                        updateStmt.setString(2, isbn);
                        updateStmt.executeUpdate();

                        // COMMIT transaction
                        conn.commit();

                        JOptionPane.showMessageDialog(this,
                                "✅ Book borrowed successfully!\nDue Date: " +
                                        java.time.LocalDate.now().plusDays(14));
                        loadBooks(""); // Refresh table
                    }
                } catch (SQLException e) {
                    // ROLLBACK on error
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            }
        }
    }

    private void reserveBook() {
        if (studentId == 0) {
            JOptionPane.showMessageDialog(this, "No student logged in!");
            return;
        }

        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book first!");
            return;
        }

        String isbn = (String) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);
        int available = (int) tableModel.getValueAt(row, 4);

        if (available > 0) {
            JOptionPane.showMessageDialog(this,
                    "Book is available! You can borrow it directly.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Reserve '" + title + "'? You'll be notified when available.",
                "Confirm Reservation", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "INSERT INTO Reservation (StudentID, ISBN, ReserveDate) VALUES (?, ?, CURDATE())";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, studentId);
                pstmt.setString(2, isbn);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Book reserved successfully!");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void deleteBook() {
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book first!");
            return;
        }

        String isbn = (String) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete '" + title + "'?\nThis action cannot be undone!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBUtil.getConnection()) {
                // Check if book is borrowed
                String checkSql = "SELECT COUNT(*) FROM BorrowRecord WHERE ISBN = ? AND Status = 'Borrowed'";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, isbn);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot delete book: it is currently borrowed!");
                    return;
                }

                // Delete book
                String deleteSql = "DELETE FROM Book WHERE ISBN = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                deleteStmt.setString(1, isbn);

                int rows = deleteStmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Book deleted successfully!");
                    loadBooks(""); // Refresh table
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
}