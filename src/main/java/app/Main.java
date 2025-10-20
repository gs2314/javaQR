package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            }

            Database database = new Database("scans.db");
            try {
                database.initialize();
            } catch (SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage(), "Database error", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            MainWindow window = new MainWindow(database);
            window.loadScans();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
