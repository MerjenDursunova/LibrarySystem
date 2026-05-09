import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class StudentDashboard extends JFrame {
    private int studentId;
    private String studentName;
    private JTabbedPane tabbedPane;
    private AIChatDialog aiChatDialog;

    public StudentDashboard(int studentId) {
        this.studentId = studentId;
        loadStudentInfo();

        setTitle("Student Dashboard - " + studentName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Create tabbed interface
        tabbedPane = new JTabbedPane();

        // Add tabs
        tabbedPane.addTab("📖 Browse Books", new BookBrowserPanel(studentId, false));
        tabbedPane.addTab("📚 My Borrowed Books", createBorrowedBooksPanel());
        tabbedPane.addTab("⏰ My Reservations", createReservationsPanel());
        tabbedPane.addTab("📊 My Profile", createProfilePanel());

        add(tabbedPane);

    // AI Chat floating button (bottom-left)
    JButton aiButton = new JButton("AI");
    aiButton.setToolTipText("Ask the Library AI assistant");
    aiButton.setBackground(new Color(255, 255, 255));
    aiButton.setFocusPainted(false);
    aiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    aiButton.addActionListener(e -> openAIChat());

    // Place the button in a small overlay panel
    JPanel overlay = new JPanel(new BorderLayout());
    overlay.setOpaque(false);
    overlay.add(aiButton, BorderLayout.WEST);
    add(overlay, BorderLayout.SOUTH);

        // Logout button in menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        // Add to constructor after creating menuBar

    }

    private void openAIChat() {
        if (aiChatDialog == null) {
            aiChatDialog = new AIChatDialog(this);
        }
        aiChatDialog.setVisible(true);
    }

    /**
     * Public helper so other panels can reuse the same chat window and send a question to it.
     */
    public void askAI(String question) {
        if (aiChatDialog == null) {
            aiChatDialog = new AIChatDialog(this);
        }
        // Ensure the dialog is visible and brought to front so the student sees it.
        aiChatDialog.setVisible(true);
        // Briefly toggle always-on-top to bring to front on some platforms
        aiChatDialog.setAlwaysOnTop(true);
        aiChatDialog.toFront();
        aiChatDialog.requestFocus();
        aiChatDialog.setAlwaysOnTop(false);
        aiChatDialog.askAndShow(question);
    }

    private void loadStudentInfo() {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT Name FROM Student WHERE StudentID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                studentName = rs.getString("Name");
            } else {
                studentName = "Unknown";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            studentName = "Error loading name";
        }
    }

    private JPanel createBorrowedBooksPanel() {
        return new BorrowReturnPanel(studentId, false);
    }

    private Object[][] getBorrowedBooksData() {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT br.BorrowID, b.Title, b.Author, br.BorrowDate, br.DueDate, br.Status, br.FineAmount
                    FROM BorrowRecord br
                    JOIN Book b ON br.ISBN = b.ISBN
                    WHERE br.StudentID = ? AND br.Status IN ('Borrowed', 'Overdue')
                    ORDER BY br.DueDate
                    """;
            PreparedStatement pstmt = conn.prepareStatement(sql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            // Get row count
            rs.last();
            int rowCount = rs.getRow();
            rs.beforeFirst();

            Object[][] data = new Object[rowCount][7];
            int i = 0;
            while (rs.next()) {
                data[i][0] = rs.getInt("BorrowID");
                data[i][1] = rs.getString("Title");
                data[i][2] = rs.getString("Author");
                data[i][3] = rs.getDate("BorrowDate");
                data[i][4] = rs.getDate("DueDate");
                data[i][5] = rs.getString("Status");
                data[i][6] = rs.getDouble("FineAmount");
                i++;
            }
            return data;
        } catch (SQLException e) {
            e.printStackTrace();
            return new Object[0][7];
        }
    }

    private void returnBook(int borrowId) {
        try (Connection conn = DBUtil.getConnection()) {
            // Calculate fine if overdue
            String sql = """
                    UPDATE BorrowRecord
                    SET ReturnDate = CURDATE(), Status = 'Returned'
                    WHERE BorrowID = ? AND StudentID = ?
                    """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, borrowId);
            pstmt.setInt(2, studentId);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Book returned successfully!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error returning book: " + e.getMessage());
        }
    }

    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Your active reservations will appear here", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM Student WHERE StudentID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                addLabelValue(panel, gbc, "Student ID:", String.valueOf(studentId), 0);
                addLabelValue(panel, gbc, "Name:", rs.getString("Name"), 1);
                addLabelValue(panel, gbc, "Email:", rs.getString("Email"), 2);
                addLabelValue(panel, gbc, "Phone:", rs.getString("Phone"), 3);

                // Statistics
                String statsSql = """
                        SELECT
                            COUNT(*) as TotalBorrows,
                            SUM(CASE WHEN Status = 'Overdue' THEN 1 ELSE 0 END) as OverdueCount,
                            SUM(FineAmount) as TotalFines
                        FROM BorrowRecord
                        WHERE StudentID = ?
                        """;
                PreparedStatement statsStmt = conn.prepareStatement(statsSql);
                statsStmt.setInt(1, studentId);
                ResultSet statsRs = statsStmt.executeQuery();

                if (statsRs.next()) {
                    addLabelValue(panel, gbc, "Total Books Borrowed:",
                            String.valueOf(statsRs.getInt("TotalBorrows")), 4);
                    addLabelValue(panel, gbc, "Overdue Books:",
                            String.valueOf(statsRs.getInt("OverdueCount")), 5);
                    addLabelValue(panel, gbc, "Total Fines:",
                            "$" + String.format("%.2f", statsRs.getDouble("TotalFines")), 6);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Buttons: Logout and Delete Account
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton logoutBtn = new JButton("Logout");
        JButton deleteBtn = new JButton("Delete Account");

        logoutBtn.addActionListener(e -> logout());

    deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to permanently delete your account? This cannot be undone.",
                    "Delete Account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Attempt to delete the student record. If there are related records, the DB may reject this.
            try (java.sql.Connection conn = DBUtil.getConnection()) {
                String sql = "DELETE FROM Student WHERE StudentID = ?";
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, studentId);
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Your account has been deleted.");
                    // Return to login window
                    SwingUtilities.invokeLater(() -> {
                        LoginWindow login = new LoginWindow();
                        login.setVisible(true);
                        this.dispose();
                    });
                } else {
                    JOptionPane.showMessageDialog(this, "Account not found or could not be deleted.");
                }
            } catch (java.sql.SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Unable to delete account. You may have active borrow/reservation records. Please contact library staff.",
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Make delete button visually prominent (red) to indicate destructive action
        try {
            deleteBtn.setForeground(Color.WHITE);
            deleteBtn.setBackground(new Color(196, 40, 28)); // red
            deleteBtn.setOpaque(true);
            deleteBtn.setBorderPainted(false);
        } catch (Exception ex) {
            // If LAF forbids background, at least set the foreground to red
            deleteBtn.setForeground(Color.RED);
        }

        btnPanel.add(deleteBtn);
        btnPanel.add(logoutBtn);
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(btnPanel, gbc);

        return panel;
    }

    private void addLabelValue(JPanel panel, GridBagConstraints gbc, String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel lbl = new JLabel(label, SwingConstants.RIGHT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(val, gbc);
    }

    private void refreshTable(JTable table, Object[][] newData) {
        String[] columns = { "Borrow ID", "Title", "Author", "Borrow Date", "Due Date", "Status", "Fine" };
        table.setModel(new javax.swing.table.DefaultTableModel(newData, columns));
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingUtilities.invokeLater(() -> {
                LoginWindow login = new LoginWindow();
                login.setVisible(true);
                this.dispose();
            });
        }
    }
}