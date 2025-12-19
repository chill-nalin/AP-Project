package edu.univ.erp.ui;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.api.auth.AuthApi;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;

public class LoginWindow extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(LoginWindow.class);

    private final AuthApi authApi;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    private static final Color BG_MAIN = new Color(10, 12, 18);
    private static final Color CARD_BG = new Color(22, 24, 32);
    private static final Color TEXT_PRIMARY = new Color(245, 245, 250);
    private static final Color TEXT_SECONDARY = new Color(150, 155, 170);
    private static final Color ACCENT = new Color(144, 238, 144);

    public LoginWindow() {
        this.authApi = new AuthApi();

        setTitle("University ERP - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        JPanel centerWrapper = new JPanel(new MigLayout("insets 0 0 0 0, wrap 1, align 50% 35%", "[grow, center]", "[]8[]"));
        centerWrapper.setOpaque(false);

        JLabel logoLabel = createLogoLabel();
        if (logoLabel != null) {
            centerWrapper.add(logoLabel, "alignx center");
        }

        JPanel card = createLoginCard();
        centerWrapper.add(card, "alignx center");

        add(centerWrapper, BorderLayout.CENTER);

        Dimension preferredSize = new Dimension(1000, 750);
        setPreferredSize(preferredSize);
        setSize(preferredSize);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
    }

    // Creates the top logo label
    private JLabel createLogoLabel() {
        try {
            URL location = getClass().getResource("/images/iiitdlogo.png");
            if (location == null) {
                log.warn("IIITD logo resource '/images/iiitdlogo.png' not found on classpath.");
                return null;
            }
            ImageIcon rawIcon = new ImageIcon(location);
            Image scaled = rawIcon.getImage().getScaledInstance(400, 80, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(scaled));
            return label;
        } catch (Exception ex) {
            log.error("Failed to load IIITD logo", ex);
            return null;
        }
    }

    // Creates the center "card" with fields and button
    private JPanel createLoginCard() {
        JPanel card = new JPanel(new MigLayout("wrap 1, fillx, insets 25 40 35 40", "[grow, 260::380]", "[]10[]20[]10[]20[]15[]"));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(40, 42, 55)));

        // Title and subtitle
        JLabel title = new JLabel("ERP Portal");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        card.add(title, "gapbottom 2");

        JLabel subtitle = new JLabel("Log in to the University ERP.");
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        card.add(subtitle, "gapbottom 2");

        // Username label + field
        JLabel lblUser = new JLabel("Username");
        lblUser.setForeground(TEXT_SECONDARY);
        lblUser.setFont(new Font("SansSerif", Font.PLAIN, 18));
        card.add(lblUser, "alignx left");

        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g., admin1");
        txtUsername.setBackground(new Color(28, 30, 40));
        txtUsername.setForeground(TEXT_PRIMARY);
        txtUsername.setCaretColor(TEXT_PRIMARY);
        txtUsername.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        card.add(txtUsername, "growx");

        // Password label + field
        JLabel lblPass = new JLabel("Password");
        lblPass.setForeground(TEXT_SECONDARY);
        lblPass.setFont(new Font("SansSerif", Font.PLAIN, 18));
        card.add(lblPass, "gaptop 2, alignx left");

        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "••••••••");
        txtPassword.setBackground(new Color(28, 30, 40));
        txtPassword.setForeground(TEXT_PRIMARY);
        txtPassword.setCaretColor(TEXT_PRIMARY);
        txtPassword.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        card.add(txtPassword, "growx");

        // Login button
        btnLogin = new JButton("Log in");
        btnLogin.setFocusPainted(false);
        btnLogin.setBackground(ACCENT);
        btnLogin.setForeground(Color.BLACK);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogin.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(btnLogin, "growx, gaptop 10");

        btnLogin.addActionListener(this::onLoginClicked);
        txtPassword.addActionListener(this::onLoginClicked);

        return card;
    }

    private void onLoginClicked(ActionEvent e) {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and Password cannot be empty.");
            return;
        }

        // Disable button to prevent double-click
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Call the API
        ApiResponse<User> response = authApi.login(username, password);

        // Re-enable button
        btnLogin.setEnabled(true);
        btnLogin.setText("Log in");

        // Check the response
        if (response.isSuccess()) {
            log.info("UI: Login successful, opening dashboard...");
            openDashboard(response.getData());
            this.dispose(); // Close the login window
        } else {
            log.warn("UI: Login failed, showing error.");
            showError(response.getMessage());
        }
    }

    private void openDashboard(User user) {
        switch (user.role()) {
            case Admin:
                log.info("Opening Admin Dashboard for {}", user.username());
                new AdminDashboard(user).setVisible(true);
                break;
            case Student:
                log.info("Opening Student Dashboard for {}", user.username());
                new StudentDashboard(user).setVisible(true);
                break;
            case Instructor:
                log.info("Opening Instructor Dashboard for {}", user.username());
                new InstructorDashboard(user).setVisible(true);
                break;
            default:
                log.error("Unknown role {} for user {}. Cannot open dashboard.", user.role(), user.username());
                showError("Your user role (" + user.role() + ") is not recognized.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login Error", JOptionPane.ERROR_MESSAGE);
    }
}
