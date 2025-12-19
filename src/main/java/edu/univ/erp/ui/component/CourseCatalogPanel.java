package edu.univ.erp.ui.component;

import edu.univ.erp.api.catalog.CatalogApi;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.student.StudentApi;
import edu.univ.erp.api.types.CourseRow;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class CourseCatalogPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(CourseCatalogPanel.class);

    private final CatalogApi catalogApi = new CatalogApi();
    private final StudentApi studentApi = new StudentApi();
    private final User currentUser;

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Color COLOR_TABLE_GRID = new Color(60, 60, 60);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_SECTION_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    private JTable table;
    private DefaultTableModel tableModel;
    private List<CourseRow> catalogData;

    public CourseCatalogPanel(User user) {
        this.currentUser = user;

        setLayout(new MigLayout(
                "wrap 1, fill, insets 20",
                "[grow,fill]",
                "[]10[]10[grow]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Title
        JLabel mainTitle = new JLabel("Course Catalog");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        // Subtitle (current term)
        JLabel lblTerm = new JLabel("Current Term Catalog");
        lblTerm.setFont(FONT_SECTION_TITLE);
        lblTerm.setForeground(COLOR_TEXT_PRIMARY);
        add(lblTerm, "growx, wrap");

        // Table
        String[] columnNames = {
                "Code", "Title", "Credits", "Instructor", "Time", "Room", "Availability"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        // Dark-theme table styling
        table.setBackground(COLOR_BACKGROUND);
        table.setForeground(COLOR_TEXT_PRIMARY);
        table.setGridColor(COLOR_TABLE_GRID);
        table.setSelectionBackground(new Color(45, 45, 45));
        table.setSelectionForeground(COLOR_TEXT_PRIMARY);
        table.setFont(FONT_LABEL);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_LABEL.deriveFont(Font.BOLD));
        header.setBackground(new Color(20, 20, 20));
        header.setForeground(COLOR_TEXT_PRIMARY);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BACKGROUND);

        add(scrollPane, "grow, wrap");

        // Actions row
        JPanel actionsPanel = new JPanel(new MigLayout(
                "insets 0, fillx",
                "[grow][]",
                "[]"
        ));
        actionsPanel.setOpaque(false);

        JLabel hintLabel = new JLabel("Select a course section from the table to register.");
        hintLabel.setForeground(COLOR_TEXT_SECONDARY);
        hintLabel.setFont(FONT_LABEL);
        actionsPanel.add(hintLabel, "growx");

        JButton btnRegister = new JButton("Register for Selected Course");
        actionsPanel.add(btnRegister, "h 32!");

        add(actionsPanel, "growx");

        btnRegister.addActionListener(e -> onRegister());

        // Initial data load
        loadCatalogData();
    }

    private void loadCatalogData() {
        log.info("Loading course catalog data...");
        tableModel.setRowCount(0);

        ApiResponse<List<CourseRow>> response = catalogApi.getCurrentCatalog();

        if (response.isSuccess()) {
            this.catalogData = response.getData();
            for (CourseRow row : catalogData) {
                tableModel.addRow(new Object[]{row.courseCode(), row.title(), row.credits(), row.instructorName(), row.dayTime(), row.room(), row.getAvailability()});
            }
            log.info("Catalog data loaded into table.");
        } else {
            log.error("Failed to load catalog: {}", response.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load course catalog: " + response.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void onRegister() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a course section from the table first.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        CourseRow selectedCourse = catalogData.get(selectedRow);
        int sectionId = selectedCourse.sectionId();
        int studentId = currentUser.userId();

        log.debug("Register button clicked for sectionId {} by studentId {}", sectionId, studentId);

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Register for " + selectedCourse.courseCode() + " - " + selectedCourse.title() + "?",
                "Confirm Registration",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            ApiResponse<Void> response = studentApi.registerForSection(studentId, sectionId);

            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(
                        this,
                        response.getMessage(),
                        "Registration Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadCatalogData();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        response.getMessage(),
                        "Registration Failed",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    @Override
    public void refreshData() {
        loadCatalogData();
    }
}
