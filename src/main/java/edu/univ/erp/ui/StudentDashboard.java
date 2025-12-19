package edu.univ.erp.ui;

import edu.univ.erp.api.maintenance.MaintenanceApi;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.User;
import edu.univ.erp.ui.component.*;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StudentDashboard extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(StudentDashboard.class);

    private static final Color SIDEBAR_BG = new Color(26, 26, 26);
    private static final Color SIDEBAR_HOVER = new Color(45, 45, 45);
    private static final Color MAIN_BG = new Color(30, 30, 30);
    private static final Color CARD_BG = new Color(26, 26, 26);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 160);
    private static final Color ACCENT_COLOR = new Color(94, 234, 212);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);

    private final User studentUser;
    private JPanel sidebarPanel;
    private JTabbedPane tabbedPane;

    public StudentDashboard(User user) {
        this.studentUser = user;
        log.info("Initializing Student Dashboard for user: {}", user.username());

        setTitle("Student Dashboard - Welcome, " + user.username());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        getContentPane().setBackground(MAIN_BG);

        createSidebar();

        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setBackground(MAIN_BG);

        checkAndShowMaintenanceBanner(mainContentPanel);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(MAIN_BG);
        tabbedPane.setForeground(TEXT_PRIMARY);

        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return 0;
            }
            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            }
        }
        );
        tabbedPane.setBorder(null);

        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index < 0) return;

            Component comp = tabbedPane.getComponentAt(index);

            // If the tab contains a JScrollPane, unwrap it
            if (comp instanceof JScrollPane scrollPane) {
                Component view = scrollPane.getViewport().getView();
                if (view instanceof Refreshable r) {
                    r.refreshData();
                }
                return;
            }

            // Direct panel
            if (comp instanceof Refreshable r) {
                r.refreshData();
            }
        });

        TimetablePanel timetablePanel = new TimetablePanel(this.studentUser);
        timetablePanel.setBackground(CARD_BG);
        tabbedPane.addTab("My Timetable", timetablePanel);

        CourseCatalogPanel catalogPanel = new CourseCatalogPanel(this.studentUser);
        catalogPanel.setBackground(CARD_BG);
        tabbedPane.addTab("Course Catalog", catalogPanel);

        MyRegistrationsPanel registrationPanel = new MyRegistrationsPanel(this.studentUser);
        registrationPanel.setBackground(CARD_BG);
        tabbedPane.addTab("My Registrations", registrationPanel);

        MyGradesPanel gradesPanel = new MyGradesPanel(this.studentUser);
        gradesPanel.setBackground(CARD_BG);
        tabbedPane.addTab("My Grades", gradesPanel);

        JPanel contentPanel = new JPanel(new MigLayout("fill, insets 10"));
        contentPanel.setBackground(MAIN_BG);
        contentPanel.add(tabbedPane, "grow");

        mainContentPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);

        Dimension preferredSize = new Dimension(1200, 800);
        setPreferredSize(preferredSize);
        setSize(preferredSize);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private void createSidebar() {
        sidebarPanel = new JPanel();
        // Uniform spacing for all nav items
        sidebarPanel.setLayout(new MigLayout("fillx, insets 0, gapy 0", "[grow]", ""));
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setPreferredSize(new Dimension(250, 0));

        JPanel brandWrapper = new JPanel(
                new MigLayout("wrap 1, fillx, insets 25 15 15 15", "[grow]", "[]10[]"));
        brandWrapper.setBackground(SIDEBAR_BG);

        ImageIcon rawIcon = new ImageIcon(getClass().getResource("/images/iiitdlogo.png"));
        Image scaledImg = rawIcon.getImage().getScaledInstance(240, 48, Image.SCALE_SMOOTH); // BIGGER
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImg));
        brandWrapper.add(logoLabel, "gapbottom 10");

        JPanel titleRow = new JPanel(new MigLayout("insets 5 0 0 0, fillx", "[grow][]", "[]"));
        titleRow.setBackground(SIDEBAR_BG);

        JLabel brandLabel = new JLabel("ERP Student");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        brandLabel.setForeground(TEXT_PRIMARY);

        titleRow.add(brandLabel, "growx");
        brandWrapper.add(titleRow, "growx");

        sidebarPanel.add(brandWrapper, "growx, wrap");

        // Navigation items
        addNavItem("📅", "My Timetable", true, 0);
        addNavItem("📚", "Course Catalog", false, 1);
        addNavItem("📝", "My Registrations", false, 2);
        addNavItem("🎓", "My Grades", false, 3);

        // Divider
        sidebarPanel.add(new JSeparator(JSeparator.HORIZONTAL) {{
            setForeground(BORDER_COLOR);
            setBackground(BORDER_COLOR);
        }}, "growx, wrap, gaptop 20, gapbottom 5");

        // Account actions inside sidebar
        addActionItem("⏻", "Logout", this::handleLogout);
        addActionItem("🔑", "Change Password", this::handleChangePassword);

        // Another divider
        sidebarPanel.add(new JSeparator(JSeparator.HORIZONTAL) {{
            setForeground(BORDER_COLOR);
            setBackground(BORDER_COLOR);
        }}, "growx, wrap, gaptop 20, gapbottom 20");

        // User info section
        JLabel userLabel = new JLabel("Logged in as");
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarPanel.add(userLabel, "gapleft 15, wrap");

        JLabel usernameLabel = new JLabel(studentUser.username());
        usernameLabel.setForeground(TEXT_PRIMARY);
        usernameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sidebarPanel.add(usernameLabel, "gapleft 15, wrap");

        JLabel roleLabel = new JLabel("Role: " + studentUser.role());
        roleLabel.setForeground(TEXT_SECONDARY);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarPanel.add(roleLabel, "gapleft 15, wrap");

        add(sidebarPanel, BorderLayout.WEST);
    }

    private void addNavItem(String icon, String text, boolean selected, int tabIndex) {
        JPanel item = new JPanel(new MigLayout("fillx, insets 8 15 8 15", "[]10[grow]"));
        item.setBackground(selected ? SIDEBAR_HOVER : SIDEBAR_BG);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(TEXT_SECONDARY);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(TEXT_PRIMARY);
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        item.add(iconLabel);
        item.add(textLabel);

        // Add selection indicator
        if (selected) {
            JPanel indicator = new JPanel();
            indicator.setBackground(ACCENT_COLOR);
            indicator.setPreferredSize(new Dimension(3, 30));
            item.add(indicator, "dock west");
        }

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tabbedPane.setSelectedIndex(tabIndex);
                updateSidebarSelection(tabIndex);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!selected) item.setBackground(SIDEBAR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!selected) item.setBackground(SIDEBAR_BG);
            }
        });

        sidebarPanel.add(item, "growx, wrap");
    }

    // Generic action-style item
    private void addActionItem(String icon, String text, Runnable action) {
        JPanel item = new JPanel(new MigLayout("fillx, insets 8 15 8 15", "[]10[grow]"));
        item.setBackground(SIDEBAR_BG);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(TEXT_SECONDARY);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(TEXT_PRIMARY);
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        item.add(iconLabel);
        item.add(textLabel);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(SIDEBAR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(SIDEBAR_BG);
            }
        });

        sidebarPanel.add(item, "growx, wrap");
    }

    private void updateSidebarSelection(int selectedIndex) {
        // Rebuild sidebar with new selection
        sidebarPanel.removeAll();
        JPanel brandWrapper = new JPanel(
                new MigLayout("wrap 1, fillx, insets 25 15 15 15", "[grow]", "[]10[]"));
        brandWrapper.setBackground(SIDEBAR_BG);

        // Logo
        ImageIcon rawIcon = new ImageIcon(getClass().getResource("/images/iiitdlogo.png"));
        Image scaledImg = rawIcon.getImage().getScaledInstance(240, 48, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImg));
        brandWrapper.add(logoLabel, "gapbottom 10");

        // Title row
        JPanel titleRow = new JPanel(new MigLayout("insets 5 0 0 0, fillx", "[grow][]", "[]"));
        titleRow.setBackground(SIDEBAR_BG);

        JLabel brandLabel = new JLabel("ERP Student");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        brandLabel.setForeground(TEXT_PRIMARY);
        titleRow.add(brandLabel, "growx");

        brandWrapper.add(titleRow, "growx");

        // Add header to sidebar
        sidebarPanel.add(brandWrapper, "growx, wrap");

        addNavItem("📅", "My Timetable", selectedIndex == 0, 0);
        addNavItem("📚", "Course Catalog", selectedIndex == 1, 1);
        addNavItem("📝", "My Registrations", selectedIndex == 2, 2);
        addNavItem("🎓", "My Grades", selectedIndex == 3, 3);

        // Divider
        sidebarPanel.add(new JSeparator(JSeparator.HORIZONTAL) {{
            setForeground(BORDER_COLOR);
            setBackground(BORDER_COLOR);
        }}, "growx, wrap, gaptop 20, gapbottom 5");

        // Account actions
        addActionItem("⏻", "Logout", this::handleLogout);
        addActionItem("🔑", "Change Password", this::handleChangePassword);

        // Divider
        sidebarPanel.add(new JSeparator(JSeparator.HORIZONTAL) {{
            setForeground(BORDER_COLOR);
            setBackground(BORDER_COLOR);
        }}, "growx, wrap, gaptop 20, gapbottom 20");

        // User info section
        JLabel userLabel = new JLabel("Logged in as");
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarPanel.add(userLabel, "gapleft 15, wrap");

        JLabel usernameLabel = new JLabel(studentUser.username());
        usernameLabel.setForeground(TEXT_PRIMARY);
        usernameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sidebarPanel.add(usernameLabel, "gapleft 15, wrap");

        JLabel roleLabel = new JLabel("Role: " + studentUser.role());
        roleLabel.setForeground(TEXT_SECONDARY);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarPanel.add(roleLabel, "gapleft 15, wrap");

        sidebarPanel.revalidate();
        sidebarPanel.repaint();
    }

    private void checkAndShowMaintenanceBanner(JPanel mainContentPanel) {
        MaintenanceApi maintenanceApi = new MaintenanceApi();
        if (maintenanceApi.isReadOnlyNow()) {
            log.warn("Maintenance mode is ON. Displaying banner.");
            JLabel banner = new JLabel(
                    "System is in READ-ONLY Maintenance Mode. Changes cannot be saved.",
                    SwingConstants.CENTER
            );
            banner.setOpaque(true);
            banner.setBackground(Color.ORANGE);
            banner.setForeground(Color.BLACK);
            banner.setFont(banner.getFont().deriveFont(Font.BOLD));
            banner.setBorder(new EmptyBorder(5, 10, 5, 10));
            mainContentPanel.add(banner, BorderLayout.NORTH);
        }
    }

    private void handleChangePassword() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this);
        dialog.setVisible(true);
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().endSession();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
        }
    }
}
