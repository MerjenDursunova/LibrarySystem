import javax.swing.*;

public class LibraryGUI {
    public static void main(String[] args) {
        // Use SwingUtilities for thread safety
        SwingUtilities.invokeLater(() -> {
            try {
                // Set look and feel to system default
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Show login window
                LoginWindow loginWindow = new LoginWindow();
                loginWindow.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}