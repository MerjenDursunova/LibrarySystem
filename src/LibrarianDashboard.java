import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LibrarianDashboard extends JFrame {
    private JTabbedPane tabbedPane;

    public LibrarianDashboard() {
        setTitle("Librarian Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // Add tabs
        tabbedPane.addTab("📚 Manage Books", new BookBrowserPanel(0, true)); // 0 = no student
        tabbedPane.addTab("➕ Add Book", new JPanel() {
            {
                JButton addBookButton = new JButton("Open Add Book Form");
                addBookButton.addActionListener(e -> {
                    AddBookDialog dialog = new AddBookDialog(LibrarianDashboard.this);
                    dialog.setVisible(true);
                });
                add(addBookButton);
            }
        });
        tabbedPane.addTab("📊 View Reports", createReportsPanel());
        tabbedPane.addTab("👥 Manage Students", createStudentsPanel());
        tabbedPane.addTab("⚠️ Overdue Books", createOverduePanel());
        tabbedPane.addTab("📋 Borrow Management", new BorrowReturnPanel(0, true));
        tabbedPane.addTab("📊 Advanced Reports", new JPanel() {
            {
                JButton reportsButton = new JButton("Open Report Generator");
                reportsButton.addActionListener(e -> {
                    ReportGenerator reports = new ReportGenerator();
                    reports.setVisible(true);
                });
                add(reportsButton);
            }
        });

        add(tabbedPane);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private JPanel createAddBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Form fields
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("ISBN:"), gbc);
        gbc.gridx = 1;
        JTextField isbnField = new JTextField(20);
        panel.add(isbnField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        JTextField titleField = new JTextField(20);
        panel.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        JTextField authorField = new JTextField(20);
        panel.add(authorField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        JTextField yearField = new JTextField(20);
        panel.add(yearField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Total Copies:"), gbc);
        gbc.gridx = 1;
        JTextField copiesField = new JTextField(20);
        panel.add(copiesField, gbc);

        // Add button
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addButton = new JButton("Add Book");
        addButton.setBackground(new Color(0, 150, 0));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> {
            addBook(isbnField.getText(), titleField.getText(), authorField.getText(),
                    yearField.getText(), copiesField.getText());
        });
        panel.add(addButton, gbc);

        return panel;
    }

    private void addBook(String isbn, String title, String author, String year, String copies) {
        try {
            int yearInt = Integer.parseInt(year);
            int copiesInt = Integer.parseInt(copies);

            try (Connection conn = DBUtil.getConnection()) {
                String sql = """
                        INSERT INTO Book (ISBN, Title, Author, Year, PublisherID, CategoryID,
                                          Total_Copies, Available_Copies)
                        VALUES (?, ?, ?, ?, 1, 1, ?, ?)
                        """;
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, isbn);
                pstmt.setString(2, title);
                pstmt.setString(3, author);
                pstmt.setInt(4, yearInt);
                pstmt.setInt(5, copiesInt);
                pstmt.setInt(6, copiesInt);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Book added successfully!");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Year and copies must be numbers!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(reportArea);

        JPanel buttonPanel = new JPanel();
        JButton popularBtn = new JButton("Most Popular Books");
        popularBtn.addActionListener(e -> showPopularBooks(reportArea));

        JButton categoryBtn = new JButton("Category Statistics");
        categoryBtn.addActionListener(e -> showCategoryStats(reportArea));

        JButton studentBtn = new JButton("Student Activity");
        studentBtn.addActionListener(e -> showStudentActivity(reportArea));

        buttonPanel.add(popularBtn);
        buttonPanel.add(categoryBtn);
        buttonPanel.add(studentBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void showPopularBooks(JTextArea area) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT b.Title, b.Author, COUNT(br.BorrowID) AS BorrowCount
                    FROM Book b
                    LEFT JOIN BorrowRecord br ON b.ISBN = br.ISBN
                    GROUP BY b.ISBN
                    ORDER BY BorrowCount DESC
                    LIMIT 10
                    """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 MOST POPULAR BOOKS\n");
            sb.append("=====================\n\n");
            sb.append(String.format("%-40s %-25s %s\n", "Title", "Author", "Borrow Count"));
            sb.append(String.format("%-40s %-25s %s\n",
                    "----------------------------------------",
                    "-------------------------",
                    "------------"));

            while (rs.next()) {
                sb.append(String.format("%-40s %-25s %d\n",
                        rs.getString("Title").length() > 40 ? rs.getString("Title").substring(0, 37) + "..."
                                : rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getInt("BorrowCount")));
            }

            area.setText(sb.toString());
        } catch (SQLException e) {
            area.setText("Error generating report: " + e.getMessage());
        }
    }

    private void showCategoryStats(JTextArea area) {
        // Similar implementation
        area.setText("Category statistics report will appear here");
    }

    private void showStudentActivity(JTextArea area) {
        // Similar implementation
        area.setText("Student activity report will appear here");
    }

    private JPanel createStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT StudentID, Name, Email, Phone FROM Student ORDER BY StudentID";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            String[] columns = { "Student ID", "Name", "Email", "Phone" };
            while (rs.next()) {
                Object[] r = new Object[4];
                r[0] = rs.getInt("StudentID");
                r[1] = rs.getString("Name");
                r[2] = rs.getString("Email");
                r[3] = rs.getString("Phone");
                rows.add(r);
            }
            Object[][] data = new Object[rows.size()][];
            for (int i = 0; i < rows.size(); i++) data[i] = rows.get(i);

            // Use a DefaultTableModel so we can refresh/update easily
            javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(data, columns) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // read-only cells
                }
            };

            JTable table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(table);
            panel.add(scrollPane, BorderLayout.CENTER);

            // Buttons for managing students
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            JButton editBtn = new JButton("Edit");
            JButton deleteBtn = new JButton("Delete");

            editBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Please select a student to edit.");
                    return;
                }
                int studentId = (int) model.getValueAt(row, 0);
                String name = (String) model.getValueAt(row, 1);
                String email = (String) model.getValueAt(row, 2);
                String phone = (String) model.getValueAt(row, 3);

                // Show edit dialog
                JDialog dlg = new JDialog(this, "Edit Student", true);
                dlg.setSize(400, 250);
                dlg.setLocationRelativeTo(this);
                JPanel p = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(8, 8, 8, 8);
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.HORIZONTAL;

                JTextField idField = new JTextField(String.valueOf(studentId));
                idField.setEditable(false);
                JTextField nameField = new JTextField(name);
                JTextField emailField = new JTextField(email);
                JTextField phoneField = new JTextField(phone);

                int r = 0;
                gbc.gridx = 0; gbc.gridy = r; p.add(new JLabel("Student ID:"), gbc);
                gbc.gridx = 1; p.add(idField, gbc); r++;
                gbc.gridx = 0; gbc.gridy = r; p.add(new JLabel("Name:"), gbc);
                gbc.gridx = 1; p.add(nameField, gbc); r++;
                gbc.gridx = 0; gbc.gridy = r; p.add(new JLabel("Email:"), gbc);
                gbc.gridx = 1; p.add(emailField, gbc); r++;
                gbc.gridx = 0; gbc.gridy = r; p.add(new JLabel("Phone:"), gbc);
                gbc.gridx = 1; p.add(phoneField, gbc); r++;

                JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JButton save = new JButton("Save");
                JButton cancel = new JButton("Cancel");
                bp.add(save); bp.add(cancel);
                gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2; p.add(bp, gbc);

                save.addActionListener(ae -> {
                    String newName = nameField.getText().trim();
                    String newEmail = emailField.getText().trim();
                    String newPhone = phoneField.getText().trim();
                    if (newName.isEmpty() || newEmail.isEmpty()) {
                        JOptionPane.showMessageDialog(dlg, "Name and Email are required");
                        return;
                    }
                    try (Connection conn2 = DBUtil.getConnection()) {
                        String upd = "UPDATE Student SET Name = ?, Email = ?, Phone = ? WHERE StudentID = ?";
                        PreparedStatement ps = conn2.prepareStatement(upd);
                        ps.setString(1, newName);
                        ps.setString(2, newEmail);
                        ps.setString(3, newPhone.isEmpty() ? null : newPhone);
                        ps.setInt(4, studentId);
                        int rrows = ps.executeUpdate();
                        if (rrows > 0) {
                            model.setValueAt(newName, row, 1);
                            model.setValueAt(newEmail, row, 2);
                            model.setValueAt(newPhone, row, 3);
                            JOptionPane.showMessageDialog(dlg, "Student updated");
                            dlg.dispose();
                        } else {
                            JOptionPane.showMessageDialog(dlg, "Update failed");
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dlg, "Database error: " + ex.getMessage());
                    }
                });

                cancel.addActionListener(ae -> dlg.dispose());

                dlg.add(p);
                dlg.setVisible(true);
            });

            deleteBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Please select a student to delete.");
                    return;
                }
                int studentId = (int) model.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Permanently delete student ID " + studentId + "? This cannot be undone.",
                        "Delete Student", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
                try (Connection conn2 = DBUtil.getConnection()) {
                    String del = "DELETE FROM Student WHERE StudentID = ?";
                    PreparedStatement ps = conn2.prepareStatement(del);
                    ps.setInt(1, studentId);
                    int rowsDel = ps.executeUpdate();
                    if (rowsDel > 0) {
                        model.removeRow(row);
                        JOptionPane.showMessageDialog(this, "Student deleted");
                    } else {
                        JOptionPane.showMessageDialog(this, "Delete failed");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Unable to delete student. They may have active records. " + ex.getMessage(),
                            "Delete Failed", JOptionPane.ERROR_MESSAGE);
                }
            });

            btns.add(editBtn);
            btns.add(deleteBtn);
            panel.add(btns, BorderLayout.SOUTH);

        } catch (SQLException e) {
            panel.add(new JLabel("Error loading students: " + e.getMessage()), BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createOverduePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Overdue books management panel", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void logout() {
        SwingUtilities.invokeLater(() -> {
            LoginWindow login = new LoginWindow();
            login.setVisible(true);
            this.dispose();
        });
    }
}