package edu.univ.erp.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 1. Set up the FlatDarkLaf Look and Feel for modern dark them.
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            log.info("FlatDarkLaf Look and Feel initialized successfully.");
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Failed to initialize FlatDarkLaf. Defaulting to standard Swing L&F.", e);
        }

        // 2. Schedule the creation and display of the Login Window on the Event Dispatch Thread (EDT)
        // This is the standard and safest way to start a Swing application.
        SwingUtilities.invokeLater(() -> {
            try {
                log.info("Application starting, creating and showing LoginWindow...");
                LoginWindow loginWindow = new LoginWindow();
                loginWindow.setVisible(true);
            } catch (Exception e) {
                log.error("CRITICAL: Failed to launch the LoginWindow!", e);
                JOptionPane.showMessageDialog(null,
                        "Failed to start the application.\nPlease check the logs for details.",
                        "Application Startup Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}