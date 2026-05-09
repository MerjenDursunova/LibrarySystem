import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AddBookDialog extends JDialog {
    private JTextField isbnField, titleField, authorField, yearField, copiesField;
    private JComboBox<String> publisherCombo, categoryCombo;
    private Map<String, Integer> publisherMap = new HashMap<>();
    private Map<String, Integer> categoryMap = new HashMap<>();

    public AddBookDialog(Frame parent) {
        super(parent, "Add New Book", true);
        setSize(500, 450);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Load publishers and categories
        loadPublishersAndCategories();

        // Form fields
        addField(mainPanel, gbc, "ISBN*:", isbnField = new JTextField(20), 0);
        addField(mainPanel, gbc, "Title*:", titleField = new JTextField(20), 1);
        addField(mainPanel, gbc, "Author*:", authorField = new JTextField(20), 2);
        addField(mainPanel, gbc, "Year:", yearField = new JTextField(20), 3);

        // Publisher dropdown
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(new JLabel("Publisher:"), gbc);
        gbc.gridx = 1;
        publisherCombo = new JComboBox<>(publisherMap.keySet().toArray(new String[0]));
        publisherCombo.insertItemAt("-- Select Publisher --", 0);
        publisherCombo.setSelectedIndex(0);
        mainPanel.add(publisherCombo, gbc);

        // Category dropdown
        gbc.gridx = 0;
        gbc.gridy = 5;
        mainPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        categoryCombo = new JComboBox<>(categoryMap.keySet().toArray(new String[0]));
        categoryCombo.insertItemAt("-- Select Category --", 0);
        categoryCombo.setSelectedIndex(0);
        mainPanel.add(categoryCombo, gbc);

        addField(mainPanel, gbc, "Total Copies*:", copiesField = new JTextField(20), 6);

        // Button panel
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));

        JButton addButton = new JButton("Add Book");
        addButton.setBackground(new Color(0, 150, 0));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addButton.addActionListener(e -> addBook());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, gbc);

        // Required fields note
        gbc.gridy = 8;
        JLabel noteLabel = new JLabel("* Required fields", SwingConstants.CENTER);
        noteLabel.setForeground(Color.RED);
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        mainPanel.add(noteLabel, gbc);

        add(mainPanel);
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String label,
            JTextField field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void loadPublishersAndCategories() {
        try (Connection conn = DBUtil.getConnection()) {
            // Load publishers
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT PublisherID, Name FROM Publisher");
            while (rs.next()) {
                publisherMap.put(rs.getString("Name"), rs.getInt("PublisherID"));
            }

            // Load categories
            rs = stmt.executeQuery("SELECT CategoryID, Name FROM Category");
            while (rs.next()) {
                categoryMap.put(rs.getString("Name"), rs.getInt("CategoryID"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addBook() {
        // Validate inputs
        if (isbnField.getText().trim().isEmpty() ||
                titleField.getText().trim().isEmpty() ||
                authorField.getText().trim().isEmpty() ||
                copiesField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (publisherCombo.getSelectedIndex() == 0 ||
                categoryCombo.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a publisher and category!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String isbn = isbnField.getText().trim();
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            int year = yearField.getText().trim().isEmpty() ? 2024 : Integer.parseInt(yearField.getText().trim());
            int totalCopies = Integer.parseInt(copiesField.getText().trim());

            String publisherName = (String) publisherCombo.getSelectedItem();
            String categoryName = (String) categoryCombo.getSelectedItem();

            int publisherId = publisherMap.get(publisherName);
            int categoryId = categoryMap.get(categoryName);

            // Check if ISBN already exists
            try (Connection conn = DBUtil.getConnection()) {
                String checkSql = "SELECT COUNT(*) FROM Book WHERE ISBN = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, isbn);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    int choice = JOptionPane.showConfirmDialog(this,
                            "Book with this ISBN already exists. Update instead?",
                            "Duplicate ISBN", JOptionPane.YES_NO_OPTION);

                    if (choice == JOptionPane.YES_OPTION) {
                        // Update existing book
                        String updateSql = """
                                UPDATE Book SET Title = ?, Author = ?, Year = ?,
                                PublisherID = ?, CategoryID = ?, Total_Copies = ?
                                WHERE ISBN = ?
                                """;
                        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        updateStmt.setString(1, title);
                        updateStmt.setString(2, author);
                        updateStmt.setInt(3, year);
                        updateStmt.setInt(4, publisherId);
                        updateStmt.setInt(5, categoryId);
                        updateStmt.setInt(6, totalCopies);
                        updateStmt.setString(7, isbn);

                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            JOptionPane.showMessageDialog(this,
                                    "✅ Book updated successfully!");
                            dispose();
                        }
                    }
                    return;
                }
            }

            // Insert new book
            try (Connection conn = DBUtil.getConnection()) {
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
                pstmt.setInt(8, totalCopies); // Initially all available

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this,
                            "✅ Book added successfully!\nISBN: " + isbn +
                                    "\nTitle: " + title + "\nAvailable copies: " + totalCopies);
                    dispose();
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Year and copies must be valid numbers!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}