import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginWindow extends JFrame {
    private JComboBox<String> userTypeCombo;
    private JTextField idField, emailField;
    private JPasswordField passwordField;
    private JPanel passwordPanel;

    public LoginWindow() {
        setTitle("📚 Library Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Header
        JLabel header = new JLabel("Library Management System", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.setForeground(new Color(0, 70, 140));
        add(header, BorderLayout.NORTH);

        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // User type
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Login as:"), gbc);
        gbc.gridx = 1;
    userTypeCombo = new JComboBox<>(new String[] { "Student", "Librarian" });
    userTypeCombo.setPreferredSize(new Dimension(240, 32));
        mainPanel.add(userTypeCombo, gbc);

        // ID Field
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
    idField = new JTextField();
    idField.setPreferredSize(new Dimension(240, 32));
        mainPanel.add(idField, gbc);

        // Email Field
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
    emailField = new JTextField();
    emailField.setPreferredSize(new Dimension(240, 32));
        mainPanel.add(emailField, gbc);

        // Password Panel (initially hidden)
        passwordPanel = new JPanel(new GridBagLayout());
        passwordPanel.setVisible(false);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.gridx = 0;
        gbc2.gridy = 0;
        passwordPanel.add(new JLabel("Password:"), gbc2);
        gbc2.gridx = 1;
    passwordField = new JPasswordField();
    passwordField.setPreferredSize(new Dimension(240, 32));
        passwordPanel.add(passwordField, gbc2);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(passwordPanel, gbc);

        // Login Button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
    JButton loginButton = new JButton("Login");
    loginButton.setBackground(new Color(0, 120, 215));
    // Make the button text color black as requested
    loginButton.setForeground(Color.BLACK);
    loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
    // Slightly larger clickable area for better UX
    loginButton.setPreferredSize(new Dimension(140, 40));
        mainPanel.add(loginButton, gbc);
        // In LoginWindow constructor, add after login button
        gbc.gridx = 0;
        gbc.gridy = 6; // Adjust row number based on your layout
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
    JButton signupButton = new JButton("New Student? Sign Up");
    signupButton.setForeground(new Color(0, 100, 200));
        signupButton.setContentAreaFilled(false);
        signupButton.setBorderPainted(false);
        signupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signupButton.addActionListener(e -> openSignupDialog());
        mainPanel.add(signupButton, gbc);
    // Demo credentials label (placed above signup)
    gbc.gridy = 5;
    gbc.gridwidth = 2;
    JLabel demoLabel = new JLabel("<html><i>Demo: Student ID 1001, Email alice@uni.edu</i></html>");
    demoLabel.setForeground(Color.DARK_GRAY);
    demoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    mainPanel.add(demoLabel, gbc);

        // Add components to frame
        add(mainPanel, BorderLayout.CENTER);

        // Event Listeners
        userTypeCombo.addActionListener(e -> togglePasswordField());
        loginButton.addActionListener(e -> performLogin());

        // Enter key support
        idField.addActionListener(e -> performLogin());
        emailField.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());
    }

    private void togglePasswordField() {
        boolean isLibrarian = userTypeCombo.getSelectedItem().equals("Librarian");
        passwordPanel.setVisible(isLibrarian);
        pack();
        setLocationRelativeTo(null);
    }

    private void performLogin() {
        String userType = (String) userTypeCombo.getSelectedItem();
        String idText = idField.getText().trim();
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter email!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (userType.equals("Student")) {
                // Student needs ID
                if (idText.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter Student ID!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int studentId = Integer.parseInt(idText);
                if (authenticateStudent(studentId, email)) {
                    openStudentDashboard(studentId);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Invalid student credentials!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else { // Librarian - ID is optional
                String password = new String(passwordField.getPassword());
                if (password.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter password!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (authenticateLibrarian(email, password)) {
                    openLibrarianDashboard();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Invalid librarian credentials!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Student ID must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean authenticateStudent(int studentId, String email) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT Name FROM Student WHERE StudentID = ? AND Email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean authenticateLibrarian(String email, String password) {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT Name FROM Librarian WHERE Email = ? AND Password = SHA2(?, 256)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openStudentDashboard(int studentId) {
        SwingUtilities.invokeLater(() -> {
            StudentDashboard dashboard = new StudentDashboard(studentId);
            dashboard.setVisible(true);
            this.dispose();
        });
    }

    private void openLibrarianDashboard() {
        SwingUtilities.invokeLater(() -> {
            LibrarianDashboard dashboard = new LibrarianDashboard();
            dashboard.setVisible(true);
            this.dispose();
        });
    }

    private void openSignupDialog() {
        JDialog signupDialog = new JDialog(this, "Student Registration", true);
        signupDialog.setSize(400, 450);
        signupDialog.setLocationRelativeTo(this);
        signupDialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Form fields
        JTextField studentIdField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField emailField = new JTextField(15);
        JTextField phoneField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JPasswordField confirmPasswordField = new JPasswordField(15);

        int row = 0;
        addFormField(formPanel, gbc, "Student ID*:", studentIdField, row++);
        addFormField(formPanel, gbc, "Full Name*:", nameField, row++);
        addFormField(formPanel, gbc, "Email*:", emailField, row++);
        addFormField(formPanel, gbc, "Phone:", phoneField, row++);
        addFormField(formPanel, gbc, "Password*:", passwordField, row++);
        addFormField(formPanel, gbc, "Confirm Password*:", confirmPasswordField, row++);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

    JButton registerButton = new JButton("Register");
    registerButton.setBackground(new Color(0, 150, 0));
    // Make the register button text black so it's readable on light backgrounds
    registerButton.setForeground(Color.BLACK);
        registerButton.addActionListener(e -> {
            if (registerStudent(studentIdField, nameField, emailField,
                    phoneField, passwordField, confirmPasswordField, signupDialog)) {
                signupDialog.dispose();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> signupDialog.dispose());

        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        formPanel.add(buttonPanel, gbc);

        // Footer note
        gbc.gridy = ++row;
        JLabel noteLabel = new JLabel("* Required fields", SwingConstants.CENTER);
        noteLabel.setForeground(Color.GRAY);
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        formPanel.add(noteLabel, gbc);

        signupDialog.add(formPanel, BorderLayout.CENTER);
        signupDialog.setVisible(true);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc,
            String label, JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private boolean registerStudent(JTextField studentIdField, JTextField nameField,
            JTextField emailField, JTextField phoneField,
            JPasswordField passwordField, JPasswordField confirmPasswordField,
            JDialog dialog) {
        // Get values
        String studentIdStr = studentIdField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Validation
        if (studentIdStr.isEmpty() || name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(dialog,
                    "Please fill in all required fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(dialog,
                    "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(dialog,
                    "Password must be at least 6 characters!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            int studentId = Integer.parseInt(studentIdStr);

            // Check if student ID already exists
            try (Connection conn = DBUtil.getConnection()) {
                // Check student ID
                String checkIdSql = "SELECT COUNT(*) FROM Student WHERE StudentID = ?";
                PreparedStatement checkIdStmt = conn.prepareStatement(checkIdSql);
                checkIdStmt.setInt(1, studentId);
                ResultSet rs = checkIdStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(dialog,
                            "Student ID already exists! Please use a different ID.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                // Check email
                String checkEmailSql = "SELECT COUNT(*) FROM Student WHERE Email = ?";
                PreparedStatement checkEmailStmt = conn.prepareStatement(checkEmailSql);
                checkEmailStmt.setString(1, email);
                rs = checkEmailStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(dialog,
                            "Email already registered! Please use a different email.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                // Insert new student
                String insertSql = "INSERT INTO Student (StudentID, Name, Email, Phone) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, studentId);
                insertStmt.setString(2, name);
                insertStmt.setString(3, email);
                insertStmt.setString(4, phone.isEmpty() ? null : phone);

                int rows = insertStmt.executeUpdate();

                if (rows > 0) {
                    // Also add to Librarian table with password (optional enhancement)
                    try {
                        String librarianSql = """
                                INSERT INTO Librarian (Name, Email, Password)
                                VALUES (?, ?, SHA2(?, 256))
                                """;
                        PreparedStatement librarianStmt = conn.prepareStatement(librarianSql);
                        librarianStmt.setString(1, name);
                        librarianStmt.setString(2, email);
                        librarianStmt.setString(3, password);
                        librarianStmt.executeUpdate();
                    } catch (SQLException e) {
                        // Ignore if duplicate - student can still login as student
                    }

                    JOptionPane.showMessageDialog(dialog,
                            "✅ Registration successful!\n\n" +
                                    "Student ID: " + studentId + "\n" +
                                    "Name: " + name + "\n" +
                                    "Email: " + email + "\n\n" +
                                    "Logging you in automatically...",
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                    // Auto-login
                    openStudentDashboard(studentId);

                    return true;
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog,
                        "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(dialog,
                    "Student ID must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return false;
    }
}