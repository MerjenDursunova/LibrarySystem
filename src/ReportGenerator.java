import javax.swing.*;
import java.awt.*;
import java.sql.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class ReportGenerator extends JFrame {
    private JTabbedPane tabbedPane;

    public ReportGenerator() {
        setTitle("📊 Library Reports");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // Add report tabs
        tabbedPane.addTab("📈 Popular Books", createPopularBooksChart());
        tabbedPane.addTab("📊 Category Distribution", createCategoryChart());
        tabbedPane.addTab("📅 Monthly Activity", createMonthlyActivityChart());
        tabbedPane.addTab("📋 Statistical Summary", createSummaryPanel());

        add(tabbedPane);
    }

    private JPanel createPopularBooksChart() {
        JPanel panel = new JPanel(new BorderLayout());

        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT b.Title, COUNT(br.BorrowID) AS BorrowCount
                    FROM Book b
                    LEFT JOIN BorrowRecord br ON b.ISBN = br.ISBN
                    GROUP BY b.ISBN
                    ORDER BY BorrowCount DESC
                    LIMIT 10
                    """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            while (rs.next()) {
                String title = rs.getString("Title");
                if (title.length() > 20) {
                    title = title.substring(0, 17) + "...";
                }
                dataset.addValue(rs.getInt("BorrowCount"), "Borrows", title);
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Top 10 Most Borrowed Books",
                    "Book Title",
                    "Number of Borrows",
                    dataset);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(950, 600));
            panel.add(chartPanel, BorderLayout.CENTER);

        } catch (SQLException e) {
            panel.add(new JLabel("Error generating chart: " + e.getMessage()),
                    BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createCategoryChart() {
        JPanel panel = new JPanel(new BorderLayout());

        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT c.Name AS Category, COUNT(b.ISBN) AS BookCount
                    FROM Category c
                    LEFT JOIN Book b ON c.CategoryID = b.CategoryID
                    GROUP BY c.CategoryID
                    ORDER BY BookCount DESC
                    """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            DefaultPieDataset dataset = new DefaultPieDataset();

            while (rs.next()) {
                dataset.setValue(rs.getString("Category"), rs.getInt("BookCount"));
            }

            JFreeChart chart = ChartFactory.createPieChart(
                    "Book Distribution by Category",
                    dataset,
                    true, // legend
                    true, // tooltips
                    false // urls
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(950, 600));
            panel.add(chartPanel, BorderLayout.CENTER);

        } catch (SQLException e) {
            panel.add(new JLabel("Error generating chart: " + e.getMessage()),
                    BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createMonthlyActivityChart() {
        JPanel panel = new JPanel(new BorderLayout());

        try (Connection conn = DBUtil.getConnection()) {
            String sql = """
                    SELECT
                        DATE_FORMAT(BorrowDate, '%Y-%m') AS Month,
                        COUNT(*) AS BorrowCount
                    FROM BorrowRecord
                    WHERE BorrowDate >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
                    GROUP BY DATE_FORMAT(BorrowDate, '%Y-%m')
                    ORDER BY Month
                    """;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            while (rs.next()) {
                dataset.addValue(rs.getInt("BorrowCount"), "Borrows", rs.getString("Month"));
            }

            JFreeChart chart = ChartFactory.createLineChart(
                    "Borrowing Activity (Last 6 Months)",
                    "Month",
                    "Number of Borrows",
                    dataset);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(950, 600));
            panel.add(chartPanel, BorderLayout.CENTER);

        } catch (SQLException e) {
            panel.add(new JLabel("Error generating chart: " + e.getMessage()),
                    BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        try (Connection conn = DBUtil.getConnection()) {
            // Get all statistics
            String[][] queries = {
                    { "Total Books", "SELECT COUNT(*) FROM Book" },
                    { "Total Students", "SELECT COUNT(*) FROM Student" },
                    { "Active Borrowings",
                            "SELECT COUNT(*) FROM BorrowRecord WHERE Status IN ('Borrowed', 'Overdue')" },
                    { "Overdue Books", "SELECT COUNT(*) FROM BorrowRecord WHERE Status = 'Overdue'" },
                    { "Total Reservations", "SELECT COUNT(*) FROM Reservation WHERE Status = 'Active'" },
                    { "Most Popular Category", """
                            SELECT c.Name, COUNT(b.ISBN) as Count
                            FROM Category c
                            JOIN Book b ON c.CategoryID = b.CategoryID
                            GROUP BY c.CategoryID
                            ORDER BY Count DESC LIMIT 1
                            """ },
                    { "Most Active Student", """
                            SELECT s.Name, COUNT(br.BorrowID) as Count
                            FROM Student s
                            JOIN BorrowRecord br ON s.StudentID = br.StudentID
                            GROUP BY s.StudentID
                            ORDER BY Count DESC LIMIT 1
                            """ }
            };

            int row = 0;
            for (String[] query : queries) {
                String label = query[0];
                String sql = query[1];

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    String value = rs.getString(1);
                    if (rs.getMetaData().getColumnCount() > 1) {
                        value = rs.getString(1) + " (" + rs.getString(2) + ")";
                    }

                    addStatistic(panel, gbc, label, value, row);
                    row++;
                }
            }

        } catch (SQLException e) {
            panel.add(new JLabel("Error loading statistics: " + e.getMessage()));
        }

        return panel;
    }

    private void addStatistic(JPanel panel, GridBagConstraints gbc,
            String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(val, gbc);
    }
}