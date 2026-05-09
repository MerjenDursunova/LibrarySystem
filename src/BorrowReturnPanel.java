import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class BorrowReturnPanel extends JPanel {
    private JTable borrowTable;
    private DefaultTableModel tableModel;
    private int studentId;
    private boolean isLibrarian;

    public BorrowReturnPanel(int studentId, boolean isLibrarian) {
        this.studentId = studentId;
        this.isLibrarian = isLibrarian;

        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel(isLibrarian ? "📊 Manage All Borrowings" : "📚 Your Borrowed Books",
                SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Table
        String[] columns = isLibrarian
                ? new String[] { "Borrow ID", "Student", "Book", "Borrow Date", "Due Date", "Status", "Fine" }
                : new String[] { "Borrow ID", "Book", "Borrow Date", "Due Date", "Status", "Fine" };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };

        borrowTable = new JTable(tableModel);
        borrowTable.setRowHeight(25);
        borrowTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        JScrollPane scrollPane = new JScrollPane(borrowTable);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton refreshButton = new JButton("🔄 Refresh");
        JButton returnButton = new JButton("📥 Return Book");
        JButton calculateFineButton = new JButton("💰 Calculate Fine");
        JButton extendButton = new JButton("📅 Extend Due Date");

        if (isLibrarian) {
            buttonPanel.add(refreshButton);
            buttonPanel.add(returnButton);
            buttonPanel.add(calculateFineButton);
            buttonPanel.add(extendButton);
        } else {
            buttonPanel.add(refreshButton);
            buttonPanel.add(returnButton);
        }

        // Add components
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load data
        loadBorrowings();

        // Event listeners
        refreshButton.addActionListener(e -> loadBorrowings());

        returnButton.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row >= 0) {
                int borrowId = (int) tableModel.getValueAt(row, 0);
                returnBook(borrowId);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a borrowing record!");
            }
        });

        calculateFineButton.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row >= 0) {
                int borrowId = (int) tableModel.getValueAt(row, 0);
                calculateFine(borrowId);
            }
        });

        extendButton.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row >= 0) {
                int borrowId = (int) tableModel.getValueAt(row, 0);
                extendDueDate(borrowId);
            }
        });
    }

    private void loadBorrowings() {
        tableModel.setRowCount(0);

        try (Connection conn = DBUtil.getConnection()) {
            String sql;
            PreparedStatement pstmt;

            if (isLibrarian) {
                sql = """
                        SELECT br.BorrowID, s.Name AS StudentName, b.Title AS BookTitle,
                               br.BorrowDate, br.DueDate, br.Status, br.FineAmount
                        FROM BorrowRecord br
                        JOIN Student s ON br.StudentID = s.StudentID
                        JOIN Book b ON br.ISBN = b.ISBN
                        ORDER BY br.BorrowDate DESC
                        """;
                pstmt = conn.prepareStatement(sql);
            } else {
                sql = """
                        SELECT br.BorrowID, b.Title AS BookTitle, br.BorrowDate,
                               br.DueDate, br.Status, br.FineAmount
                        FROM BorrowRecord br
                        JOIN Book b ON br.ISBN = b.ISBN
                        WHERE br.StudentID = ?
                        ORDER BY br.DueDate
                        """;
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, studentId);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                if (isLibrarian) {
                    Object[] row = {
                            rs.getInt("BorrowID"),
                            rs.getString("StudentName"),
                            rs.getString("BookTitle"),
                            rs.getDate("BorrowDate"),
                            rs.getDate("DueDate"),
                            getStatusWithIcon(rs.getString("Status")),
                            String.format("$%.2f", rs.getDouble("FineAmount"))
                    };
                    tableModel.addRow(row);
                } else {
                    Object[] row = {
                            rs.getInt("BorrowID"),
                            rs.getString("BookTitle"),
                            rs.getDate("BorrowDate"),
                            rs.getDate("DueDate"),
                            getStatusWithIcon(rs.getString("Status")),
                            String.format("$%.2f", rs.getDouble("FineAmount"))
                    };
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading borrowings: " + e.getMessage());
        }
    }

    private String getStatusWithIcon(String status) {
        switch (status) {
            case "Borrowed":
                return "📗 " + status;
            case "Returned":
                return "📘 " + status;
            case "Overdue":
                return "⚠️ " + status;
            default:
                return status;
        }
    }

    private void returnBook(int borrowId) {
        try (Connection conn = DBUtil.getConnection()) {
            // Get book info for confirmation
            String infoSql = """
                    SELECT b.Title, br.DueDate, DATEDIFF(CURDATE(), br.DueDate) AS DaysOverdue
                    FROM BorrowRecord br
                    JOIN Book b ON br.ISBN = b.ISBN
                    WHERE br.BorrowID = ? AND br.Status IN ('Borrowed', 'Overdue')
                    """;
            PreparedStatement infoStmt = conn.prepareStatement(infoSql);
            infoStmt.setInt(1, borrowId);
            ResultSet rs = infoStmt.executeQuery();

            if (rs.next()) {
                String title = rs.getString("Title");
                int daysOverdue = rs.getInt("DaysOverdue");
                double fine = daysOverdue > 0 ? daysOverdue * 0.50 : 0.0;

                String message = "Return '" + title + "'?";
                if (fine > 0) {
                    message += "\n⚠️ Overdue by " + daysOverdue + " days. Fine: $" +
                            String.format("%.2f", fine);
                }

                int confirm = JOptionPane.showConfirmDialog(this, message,
                        "Confirm Return", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    conn.setAutoCommit(false);
                    try {
                        // Update borrow record (trigger will handle Available_Copies)
                        String updateSql = """
                                UPDATE BorrowRecord
                                SET ReturnDate = CURDATE(), Status = 'Returned', FineAmount = ?
                                WHERE BorrowID = ? AND Status IN ('Borrowed', 'Overdue')
                                """;
                        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        updateStmt.setDouble(1, fine);
                        updateStmt.setInt(2, borrowId);

                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            conn.commit();
                            JOptionPane.showMessageDialog(this,
                                    "✅ Book returned successfully!" +
                                            (fine > 0 ? "\nFine applied: $" + String.format("%.2f", fine) : ""));
                            // REFRESH THE TABLE
                            loadBorrowings();
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Book already returned or not found!");
                        }
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Book already returned or not found!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error returning book: " + e.getMessage());
        }
    }

    private void calculateFine(int borrowId) {
        try (Connection conn = DBUtil.getConnection()) {
            // Compute fine in Java to avoid DB stored-procedure dependency
            String sql = "SELECT DueDate, Status FROM BorrowRecord WHERE BorrowID = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, borrowId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Borrow record not found.");
                return;
            }

            java.sql.Date due = rs.getDate("DueDate");
            String status = rs.getString("Status");
            java.time.LocalDate dueDate = due.toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
            if (daysOverdue < 0) daysOverdue = 0;
            double rate = 0.50; // $0.50 per day
            double fine = daysOverdue * rate;

            String msg;
            if (daysOverdue == 0) {
                msg = "No fine. This item is not overdue.";
            } else {
                msg = String.format("Overdue by %d days. Calculated fine: $%.2f", daysOverdue, fine);
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Calculate fine for Borrow ID " + borrowId + "?\n" + msg,
                    "Calculate Fine", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Persist the fine and update status if needed
            String upd = "UPDATE BorrowRecord SET FineAmount = ?, Status = ? WHERE BorrowID = ?";
            PreparedStatement ups = conn.prepareStatement(upd);
            String newStatus = (daysOverdue > 0) ? "Overdue" : status;
            ups.setDouble(1, fine);
            ups.setString(2, newStatus);
            ups.setInt(3, borrowId);
            int u = ups.executeUpdate();
            if (u > 0) {
                JOptionPane.showMessageDialog(this, "Fine calculated and saved: $" + String.format("%.2f", fine));
                loadBorrowings();
            } else {
                JOptionPane.showMessageDialog(this, "Unable to save fine.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error calculating fine: " + e.getMessage());
        }
    }

    private void extendDueDate(int borrowId) {
        String daysStr = JOptionPane.showInputDialog(this,
                "Extend due date by how many days?");

        if (daysStr != null && !daysStr.trim().isEmpty()) {
            try {
                int days = Integer.parseInt(daysStr);

                try (Connection conn = DBUtil.getConnection()) {
                    String sql = """
                            UPDATE BorrowRecord
                            SET DueDate = DATE_ADD(DueDate, INTERVAL ? DAY)
                            WHERE BorrowID = ?
                            """;
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, days);
                    pstmt.setInt(2, borrowId);

                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this,
                                "✅ Due date extended by " + days + " days!");
                        loadBorrowings();
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
}