package edu.univ.erp.ui.component;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.student.StudentApi;
import edu.univ.erp.api.types.RegistrationRow;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class MyRegistrationsPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(MyRegistrationsPanel.class);

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
    private List<RegistrationRow> registrationData;

    public MyRegistrationsPanel(User user) {
        this.currentUser = user;

        setLayout(new MigLayout(
                "wrap 1, fill, insets 20",
                "[grow,fill]",
                "[]10[]10[grow]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Title
        JLabel mainTitle = new JLabel("My Registered Sections");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        // Subtitle
        JLabel subTitle = new JLabel("Current Registrations");
        subTitle.setFont(FONT_SECTION_TITLE);
        subTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(subTitle, "growx, wrap");

        // Table
        String[] columnNames = {"Course Code", "Title", "Instructor", "Time", "Room", "Status"};
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

        JPanel actionsPanel = new JPanel(new MigLayout(
                "insets 0, fillx",
                "[grow][]",
                "[]"
        ));
        actionsPanel.setOpaque(false);

        JLabel hintLabel = new JLabel("Select a registered section and click Drop to remove it.");
        hintLabel.setForeground(COLOR_TEXT_SECONDARY);
        hintLabel.setFont(FONT_LABEL);
        actionsPanel.add(hintLabel, "growx");

        JButton btnDrop = new JButton("Drop Selected Section");
        actionsPanel.add(btnDrop, "h 32!");

        add(actionsPanel, "growx");

        btnDrop.addActionListener(e -> onDrop());

        loadRegistrationData();
    }

    // Data loading
    private void loadRegistrationData() {
        log.info("Loading registrations for student {}", currentUser.userId());
        tableModel.setRowCount(0);
        ApiResponse<List<RegistrationRow>> response =
                studentApi.getMyRegistrations(currentUser.userId());

        if (response.isSuccess()) {
            this.registrationData = response.getData();
            if (registrationData.isEmpty()) {
                tableModel.addRow(new Object[]{
                        "", "You are not registered for any sections.", "", "", "", ""
                });
            } else {
                for (RegistrationRow row : registrationData) {
                    tableModel.addRow(new Object[]{
                            row.courseCode(),
                            row.title(),
                            row.instructorName(),
                            row.dayTime(),
                            row.room(),
                            row.status()
                    });
                }
            }
            log.info("Registration data loaded into table.");
        } else {
            log.error("Failed to load registrations: {}", response.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load your registrations: " + response.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Drop action
    private void onDrop() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1
                || registrationData == null
                || registrationData.isEmpty()
                || selectedRow >= registrationData.size()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a section to drop from the table first.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        RegistrationRow selectedReg = registrationData.get(selectedRow);
        int enrollmentId = selectedReg.enrollmentId();
        log.debug("Drop button clicked for enrollmentId {} by studentId {}",
                enrollmentId, currentUser.userId());

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to drop " +
                        selectedReg.courseCode() + " - " + selectedReg.title() + "?",
                "Confirm Drop",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            ApiResponse<Void> response =
                    studentApi.dropSection(currentUser.userId(), enrollmentId);
            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(
                        this,
                        response.getMessage(),
                        "Drop Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadRegistrationData();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        response.getMessage(),
                        "Drop Failed",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    @Override
    public void refreshData() {
        loadRegistrationData();
    }
}
