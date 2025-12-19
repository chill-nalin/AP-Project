package edu.univ.erp.ui;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.instructor.InstructorApi;
import edu.univ.erp.api.maintenance.MaintenanceApi;
import edu.univ.erp.api.types.InstructorSectionRow;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.SettingsRepository;
import edu.univ.erp.domain.User;
import edu.univ.erp.ui.component.GradebookPanel;
import edu.univ.erp.ui.component.StatisticsPanel;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

public class InstructorDashboard extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(InstructorDashboard.class);

    private static final Color SIDEBAR_BG = new Color(26, 26, 26);
    private static final Color SIDEBAR_HOVER = new Color(45, 45, 45);
    private static final Color MAIN_BG = new Color(30, 30, 30);
    private static final Color CARD_BG = new Color(45, 45, 45);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 160);
    private static final Color ACCENT_COLOR = new Color(94, 234, 212);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);

    private final User instructorUser;

    private JPanel sidebarPanel;
    private JPanel sectionsListPanel;
    private JPanel rightPanel;

    private JPanel selectedSectionItem;

    private final InstructorApi instructorApi = new InstructorApi();
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private List<InstructorSectionRow> sectionData = Collections.emptyList();

    public InstructorDashboard(User user) {
        this.instructorUser = user;
        log.info("Initializing Instructor Dashboard for user: {}", user.username());

        setTitle("Instructor Dashboard - Welcome, " + user.username());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        getContentPane().setBackground(MAIN_BG);

        createSidebar();

        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 0));
        mainContentPanel.setBackground(MAIN_BG);

        checkAndShowMaintenanceBanner(mainContentPanel);

        rightPanel = new JPanel(new MigLayout("fill, wrap 1", "[grow]", "[grow]0[shrink]"));
        rightPanel.setBackground(MAIN_BG);

        JPanel gradebookPlaceholder = new JPanel(new BorderLayout());
        gradebookPlaceholder.setBackground(CARD_BG);
        gradebookPlaceholder.setBorder(BorderFactory.createTitledBorder("Gradebook"));
        JLabel gbLabel = new JLabel("Select a section from the left.", SwingConstants.CENTER);
        gbLabel.setForeground(TEXT_SECONDARY);
        gradebookPlaceholder.add(gbLabel, BorderLayout.CENTER);
        rightPanel.add(gradebookPlaceholder, "grow");

        JPanel statsPlaceholder = new JPanel(new BorderLayout());
        statsPlaceholder.setBackground(CARD_BG);
        statsPlaceholder.setBorder(BorderFactory.createTitledBorder("Class Averages"));
        JLabel stLabel = new JLabel("Statistics will appear here.", SwingConstants.CENTER);
        stLabel.setForeground(TEXT_SECONDARY);
        statsPlaceholder.add(stLabel, BorderLayout.CENTER);
        rightPanel.add(statsPlaceholder, "growx, height 220!");

        mainContentPanel.add(rightPanel, BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);

        Dimension preferredSize = new Dimension(1200, 800);
        setPreferredSize(preferredSize);
        setSize(preferredSize);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private void createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new MigLayout("fillx, insets 0, gapy 0", "[grow]", ""));
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setPreferredSize(new Dimension(280, 0));

        JPanel brandWrapper = new JPanel(
                new MigLayout("wrap 1, fillx, insets 25 15 15 15", "[grow]", "[]10[]"));
        brandWrapper.setBackground(SIDEBAR_BG);

        ImageIcon rawIcon = new ImageIcon(getClass().getResource("/images/iiitdlogo.png"));
        Image scaledImg = rawIcon.getImage().getScaledInstance(240, 48, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImg));
        brandWrapper.add(logoLabel, "gapbottom 10");

        JPanel titleRow = new JPanel(new MigLayout("insets 5 0 0 0, fillx", "[grow][]", "[]"));
        titleRow.setBackground(SIDEBAR_BG);

        JLabel brandLabel = new JLabel("ERP Instructor");
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        brandLabel.setForeground(TEXT_PRIMARY);
        titleRow.add(brandLabel, "growx");
        brandWrapper.add(titleRow, "growx");

        sidebarPanel.add(brandWrapper, "growx, wrap");

        String currentSemester = settingsRepo.getCurrentSemester();
        int currentYear = settingsRepo.getCurrentYear();
        String termText = currentSemester + " " + currentYear;

        JPanel sectionsHeader = new JPanel(
                new MigLayout("insets 10 15 5 15, fillx", "[]10[][grow]", "[]"));
        sectionsHeader.setBackground(SIDEBAR_BG);

        JLabel bookIcon = new JLabel("📘");
        bookIcon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        bookIcon.setForeground(TEXT_PRIMARY);
        sectionsHeader.add(bookIcon);

        JLabel sectionsTitle = new JLabel("My Sections");
        sectionsTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        sectionsTitle.setForeground(TEXT_PRIMARY);
        sectionsHeader.add(sectionsTitle);

        JLabel termLabel = new JLabel("(" + termText + ")");
        termLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        termLabel.setForeground(TEXT_SECONDARY);
        sectionsHeader.add(termLabel, "gapleft 6");

        sidebarPanel.add(sectionsHeader, "growx, wrap");

        sectionsListPanel = new JPanel(
                new MigLayout("fillx, insets 0 35 0 15, gapy 0", "[grow]", ""));
        sectionsListPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.add(sectionsListPanel, "growx, wrap");

        loadSectionsIntoSidebar();

        sidebarPanel.add(new JSeparator(JSeparator.HORIZONTAL) {{
            setForeground(BORDER_COLOR);
            setBackground(BORDER_COLOR);
        }}, "growx, wrap, gaptop 20, gapbottom 5");

        addActionItem("⏻", "Logout", this::handleLogout);
        addActionItem("🔑", "Change Password", this::handleChangePassword);

        sidebarPanel.add(new JSeparator(JSeparator.HORIZONTAL) {{
            setForeground(BORDER_COLOR);
            setBackground(BORDER_COLOR);
        }}, "growx, wrap, gaptop 20, gapbottom 20");

        JLabel userLabel = new JLabel("Logged in as");
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarPanel.add(userLabel, "gapleft 15, wrap");

        JLabel usernameLabel = new JLabel(instructorUser.username());
        usernameLabel.setForeground(TEXT_PRIMARY);
        usernameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sidebarPanel.add(usernameLabel, "gapleft 15, wrap");

        JLabel roleLabel = new JLabel("Role: Instructor");
        roleLabel.setForeground(TEXT_SECONDARY);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sidebarPanel.add(roleLabel, "gapleft 15, wrap");

        add(sidebarPanel, BorderLayout.WEST);
    }

    // Load sections and populate sectionsListPanel as sub-menu items
    private void loadSectionsIntoSidebar() {
        sectionsListPanel.removeAll();
        selectedSectionItem = null;

        log.info("Loading sections for instructor {}", instructorUser.userId());

        ApiResponse<List<InstructorSectionRow>> response = instructorApi.getMySections();

        if (response.isSuccess()) {
            sectionData = response.getData();
            if (sectionData == null || sectionData.isEmpty()) {
                JLabel empty = new JLabel("No sections assigned for this term.");
                empty.setForeground(TEXT_SECONDARY);
                empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 13f));
                sectionsListPanel.add(empty, "growx, wrap");
            } else {
                int idx = 0;
                for (InstructorSectionRow row : sectionData) {
                    addSectionSidebarItem(row, idx++);
                }
            }
        } else {
            sectionData = Collections.emptyList();
            JLabel errorLabel = new JLabel("Error loading sections.");
            errorLabel.setForeground(TEXT_SECONDARY);
            errorLabel.setFont(errorLabel.getFont().deriveFont(Font.ITALIC, 13f));
            sectionsListPanel.add(errorLabel, "growx, wrap");

            JOptionPane.showMessageDialog(this,
                    "Could not load assigned sections: " + response.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        sectionsListPanel.revalidate();
        sectionsListPanel.repaint();
    }

    private void addSectionSidebarItem(InstructorSectionRow row, int index) {
        String labelText = row.courseCode() + " – " + row.courseTitle();

        JPanel item = new JPanel(new MigLayout("fillx, insets 4 5 4 5", "[grow]", "[]"));
        item.setBackground(SIDEBAR_BG);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        item.add(label, "growx");

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showSectionDetails(row);
                highlightSelectedSection(item);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (item != selectedSectionItem) {
                    item.setBackground(SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (item != selectedSectionItem) {
                    item.setBackground(SIDEBAR_BG);
                }
            }
        });

        sectionsListPanel.add(item, "growx, wrap");
    }

    // Visually highlight the selected section item
    private void highlightSelectedSection(JPanel selectedItem) {
        // Clear previous selection
        if (selectedSectionItem != null) {
            selectedSectionItem.setBorder(null);
            selectedSectionItem.setBackground(SIDEBAR_BG);
        }

        // Mark new selection
        selectedSectionItem = selectedItem;
        selectedSectionItem.setBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT_COLOR));
        selectedSectionItem.setBackground(SIDEBAR_HOVER);

        sectionsListPanel.revalidate();
        sectionsListPanel.repaint();
    }

    private void showSectionDetails(InstructorSectionRow section) {
        int sectionId = section.sectionId();
        log.debug("Section selected: {} (ID: {})", section.courseCode(), sectionId);

        rightPanel.removeAll();

        GradebookPanel gradebookPanel = new GradebookPanel(sectionId);
        rightPanel.add(gradebookPanel, "grow");

        StatisticsPanel statsPanel = new StatisticsPanel(sectionId);
        rightPanel.add(statsPanel, "growx, height 220!");

        rightPanel.revalidate();
        rightPanel.repaint();
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

    private void addActionItem(String icon, String text, Runnable action) {
        JPanel item = new JPanel(new MigLayout("fillx, insets 8 15 8 15", "[]10[grow]", "[]"));
        item.setBackground(SIDEBAR_BG);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(TEXT_SECONDARY);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        item.add(iconLabel);

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(TEXT_PRIMARY);
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        item.add(textLabel, "growx");

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
