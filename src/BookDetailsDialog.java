import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BookDetailsDialog extends JDialog {
    private String isbn;
    private String title;
    private String author;

    /**
     * @param parent owning frame
     * @param isbn book isbn
     * @param autoAsk if true, will open the AI chat and ask about the book automatically
     */
    public BookDetailsDialog(Frame parent, String isbn, boolean autoAsk) {
    super(parent, "Book Details", false); // non-modal so AI can appear alongside
    this.isbn = isbn;
    setSize(500, 400);
    setLocationRelativeTo(parent);

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT b.*, c.Name AS Category, p.Name AS Publisher,
                           (SELECT COUNT(*) FROM BorrowRecord br
                            WHERE br.ISBN = b.ISBN) AS TotalBorrows
                    FROM Book b
                    JOIN Category c ON b.CategoryID = c.CategoryID
                    JOIN Publisher p ON b.PublisherID = p.PublisherID
                    WHERE b.ISBN = ?
                    """;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Info panel
                JPanel infoPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.anchor = GridBagConstraints.WEST;
        this.title = rs.getString("Title");
        this.author = rs.getString("Author");

        addLabelValue(infoPanel, gbc, "ISBN:", rs.getString("ISBN"), 0);
        addLabelValue(infoPanel, gbc, "Title:", this.title, 1);
        addLabelValue(infoPanel, gbc, "Author:", this.author, 2);
        addLabelValue(infoPanel, gbc, "Year:", String.valueOf(rs.getInt("Year")), 3);
        addLabelValue(infoPanel, gbc, "Category:", rs.getString("Category"), 4);
        addLabelValue(infoPanel, gbc, "Publisher:", rs.getString("Publisher"), 5);
        addLabelValue(infoPanel, gbc, "Total Copies:",
            String.valueOf(rs.getInt("Total_Copies")), 6);
        addLabelValue(infoPanel, gbc, "Available:",
            String.valueOf(rs.getInt("Available_Copies")), 7);
        addLabelValue(infoPanel, gbc, "Times Borrowed:",
            String.valueOf(rs.getInt("TotalBorrows")), 8);

                // Status indicator
                String status = rs.getInt("Available_Copies") > 0 ? "✅ Available" : "⛔ Checked Out";
                addLabelValue(infoPanel, gbc, "Status:", status, 9);

                panel.add(infoPanel, BorderLayout.CENTER);

                // Cover image placeholder
                JLabel coverLabel = new JLabel("📖", SwingConstants.CENTER);
                coverLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
                coverLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                coverLabel.setPreferredSize(new Dimension(100, 140));

                JPanel leftPanel = new JPanel(new BorderLayout());
                leftPanel.add(coverLabel, BorderLayout.NORTH);
                leftPanel.add(new JPanel(), BorderLayout.CENTER); // Spacer

                // Main layout with image on left
                JPanel mainPanel = new JPanel(new BorderLayout(15, 0));
                mainPanel.add(leftPanel, BorderLayout.WEST);
                mainPanel.add(infoPanel, BorderLayout.CENTER);

                panel.add(mainPanel, BorderLayout.CENTER);

            } else {
                panel.add(new JLabel("Book not found!", SwingConstants.CENTER),
                        BorderLayout.CENTER);
            }
        } catch (SQLException e) {
            panel.add(new JLabel("Error loading book details: " + e.getMessage(),
                    SwingConstants.CENTER), BorderLayout.CENTER);
        }

        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        // Ask AI button
        JButton askButton = new JButton("Ask AI about this book");
        askButton.addActionListener(e -> askAI());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(askButton);
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        // If autoAsk requested, perform it (after showing so the chat can appear)
        if (autoAsk) {
            // show the dialog non-modally and then trigger the AI
            setVisible(true);
            askAI();
            return;
        }
    }

    private void addLabelValue(JPanel panel, GridBagConstraints gbc,
            String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel lbl = new JLabel(label, SwingConstants.RIGHT);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(val, gbc);
    }

    private void askAI() {
        String question = String.format("Tell me about the book '%s' by %s (ISBN: %s). What is it about and who would benefit from reading it?",
                title == null ? "" : title,
                author == null ? "" : author,
                isbn == null ? "" : isbn);
        // Prefer the dialog owner (the frame that created this dialog)
        Window owner = getOwner();
        if (owner instanceof StudentDashboard sd) {
            sd.askAI(question);
            return;
        }

        // Fallback: if owner is a frame, open a local chat; otherwise open without owner
        Frame frameOwner = null;
        if (owner instanceof Frame) frameOwner = (Frame) owner;
        AIChatDialog chat = new AIChatDialog(frameOwner);
        chat.setVisible(true);
        chat.askAndShow(question);
    }
}