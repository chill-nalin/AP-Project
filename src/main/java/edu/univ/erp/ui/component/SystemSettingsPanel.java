package edu.univ.erp.ui.component;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import edu.univ.erp.access.AccessControl;
import edu.univ.erp.api.admin.AdminApi;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.maintenance.MaintenanceApi;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

// JPanel for administrative settings like Maintenance Mode, Deadlines, and Backup/Restore.

public class SystemSettingsPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsPanel.class);
    private final MaintenanceApi maintenanceApi = new MaintenanceApi();
    private final AdminApi adminApi = new AdminApi();
    private final AccessControl accessControl = new AccessControl();

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_SECTION_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    private JCheckBox chkMaintenanceMode;
    private JLabel lblStatus;

    private DatePicker datePickerDropDeadline;
    private JLabel lblCurrentDropDeadline;

    private DatePicker datePickerRegDeadline;
    private JLabel lblCurrentRegDeadline;
    private JButton btnSetRegDeadline;
    private JButton btnSetDropDeadline;

    private JButton btnBackup;
    private JButton btnRestore;

    private boolean isProgrammaticallyUpdating = false;

    public SystemSettingsPanel() {
        setLayout(new MigLayout(
                "wrap 1, fillx, insets 20",
                "[grow,fill]",
                "[]15[]15[]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Main title
        JLabel mainTitle = new JLabel("System Settings");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        // Maintenance Mode
        JLabel lblMaintTitle = new JLabel("Maintenance Mode");
        lblMaintTitle.setFont(FONT_SECTION_TITLE);
        lblMaintTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(lblMaintTitle, "growx");

        JPanel maintenancePanel = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[grow,fill][]",
                "[]10[]"
        ));
        maintenancePanel.setOpaque(false);

        chkMaintenanceMode = new JCheckBox("Enable Maintenance Mode (Read-Only)");
        chkMaintenanceMode.setForeground(COLOR_TEXT_PRIMARY);
        chkMaintenanceMode.setBackground(COLOR_BACKGROUND);

        lblStatus = new JLabel("Current Status: Unknown");
        lblStatus.setForeground(COLOR_TEXT_SECONDARY);
        lblStatus.setFont(FONT_LABEL);

        chkMaintenanceMode.addActionListener(e -> {
            if (isProgrammaticallyUpdating) return;
            boolean shouldBeEnabled = chkMaintenanceMode.isSelected();
            log.debug("Maintenance checkbox clicked. New desired state: {}", shouldBeEnabled);
            setMaintenanceMode(shouldBeEnabled);
        });

        maintenancePanel.add(chkMaintenanceMode, "span 2, wrap");
        maintenancePanel.add(createLabel("Status:"), "split 2");
        maintenancePanel.add(lblStatus, "growx, align left");

        add(maintenancePanel, "growx, wrap");

        // Registration Deadline
        JLabel lblRegTitle = new JLabel("Course Registration Deadline");
        lblRegTitle.setFont(FONT_SECTION_TITLE);
        lblRegTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(lblRegTitle, "growx");

        JPanel regDeadlinePanel = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,120:pref]15[grow,fill]",
                "[]10[]"
        ));
        regDeadlinePanel.setOpaque(false);

        regDeadlinePanel.add(createLabel("Current Deadline:"));
        lblCurrentRegDeadline = new JLabel("Not Set");
        lblCurrentRegDeadline.setForeground(COLOR_TEXT_SECONDARY);
        lblCurrentRegDeadline.setFont(FONT_LABEL);
        regDeadlinePanel.add(lblCurrentRegDeadline, "growx, wrap");

        regDeadlinePanel.add(createLabel("Set New Deadline:"));
        DatePickerSettings regDateSettings = new DatePickerSettings();
        regDateSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        regDateSettings.setAllowKeyboardEditing(false);
        datePickerRegDeadline = new DatePicker(regDateSettings);
        regDeadlinePanel.add(datePickerRegDeadline, "split 2, growx");

        btnSetRegDeadline = new JButton("Set Deadline");
        regDeadlinePanel.add(btnSetRegDeadline);
        btnSetRegDeadline.addActionListener(e -> setRegistrationDeadline());

        add(regDeadlinePanel, "growx, wrap");

        // Drop Deadline
        JLabel lblDropTitle = new JLabel("Course Drop Deadline");
        lblDropTitle.setFont(FONT_SECTION_TITLE);
        lblDropTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(lblDropTitle, "growx");

        JPanel dropDeadlinePanel = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,120:pref]15[grow,fill]",
                "[]10[]"
        ));
        dropDeadlinePanel.setOpaque(false);

        dropDeadlinePanel.add(createLabel("Current Deadline:"));
        lblCurrentDropDeadline = new JLabel("Not Set");
        lblCurrentDropDeadline.setForeground(COLOR_TEXT_SECONDARY);
        lblCurrentDropDeadline.setFont(FONT_LABEL);
        dropDeadlinePanel.add(lblCurrentDropDeadline, "growx, wrap");

        dropDeadlinePanel.add(createLabel("Set New Deadline:"));
        DatePickerSettings dropDateSettings = new DatePickerSettings();
        dropDateSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        dropDateSettings.setAllowKeyboardEditing(false);
        datePickerDropDeadline = new DatePicker(dropDateSettings);
        dropDeadlinePanel.add(datePickerDropDeadline, "split 2, growx");

        btnSetDropDeadline = new JButton("Set Deadline");
        dropDeadlinePanel.add(btnSetDropDeadline);
        btnSetDropDeadline.addActionListener(e -> setDropDeadline());

        add(dropDeadlinePanel, "growx, wrap");

        refreshStatus();
    }

    // Helper label styling
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT_SECONDARY);
        label.setFont(FONT_LABEL);
        return label;
    }

    // Calls the API to set the maintenance mode state in a background thread.
    private void setMaintenanceMode(boolean desiredState) {
        log.info("UI: Requesting to set maintenance mode to: {}", desiredState);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        chkMaintenanceMode.setEnabled(false);

        SwingWorker<ApiResponse<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected ApiResponse<Void> doInBackground() throws Exception {
                return maintenanceApi.setMaintenanceMode(desiredState);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (!response.isSuccess()) {
                        JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                                response.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        isProgrammaticallyUpdating = true;
                        try {
                            chkMaintenanceMode.setSelected(!desiredState);
                        } finally {
                            isProgrammaticallyUpdating = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error during maintenance mode update worker", e);
                    JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                            "An unexpected error occurred: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    chkMaintenanceMode.setEnabled(true);
                    refreshStatus();
                }
            }
        };
        worker.execute();
    }

    // Refreshes the display of maintenance mode and deadlines by fetching current value from the backend in a background thread.

    private void refreshStatus() {
        log.debug("Refreshing all statuses...");
        chkMaintenanceMode.setEnabled(false);
        datePickerRegDeadline.setEnabled(false);
        btnSetRegDeadline.setEnabled(false);
        datePickerDropDeadline.setEnabled(false);
        btnSetDropDeadline.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean currentMaintenanceState;
            private LocalDate currentDropDeadline;
            private String dropDeadlineError = null;
            private LocalDate currentRegDeadline;
            private String regDeadlineError = null;

            @Override
            protected Void doInBackground() throws Exception {
                currentMaintenanceState = maintenanceApi.isReadOnlyNow();

                ApiResponse<LocalDate> dropDeadlineResponse = maintenanceApi.getDropDeadline();
                if (dropDeadlineResponse.isSuccess()) {
                    currentDropDeadline = dropDeadlineResponse.getData();
                } else {
                    dropDeadlineError = dropDeadlineResponse.getMessage();
                }

                ApiResponse<LocalDate> regDeadlineResponse = maintenanceApi.getRegistrationDeadline();
                if (regDeadlineResponse.isSuccess()) {
                    currentRegDeadline = regDeadlineResponse.getData();
                } else {
                    regDeadlineError = regDeadlineResponse.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Maintenance state
                    isProgrammaticallyUpdating = true;
                    try {
                        chkMaintenanceMode.setSelected(currentMaintenanceState);
                    } finally {
                        isProgrammaticallyUpdating = false;
                    }
                    lblStatus.setText("Current Status: " +
                            (currentMaintenanceState ? "ON (Read-Only)" : "OFF (Normal)"));
                    lblStatus.setForeground(
                            currentMaintenanceState ? new Color(248, 113, 113) : new Color(56, 189, 248)
                    );
                    // Deadlines
                    updateDeadlineLabel(lblCurrentDropDeadline, datePickerDropDeadline,
                            currentDropDeadline, dropDeadlineError, "drop");
                    updateDeadlineLabel(lblCurrentRegDeadline, datePickerRegDeadline,
                            currentRegDeadline, regDeadlineError, "registration");

                } catch (Exception e) {
                    log.error("Error refreshing statuses", e);
                    JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                            "Failed to refresh settings status: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    chkMaintenanceMode.setEnabled(true);
                    datePickerRegDeadline.setEnabled(true);
                    btnSetRegDeadline.setEnabled(true);
                    datePickerDropDeadline.setEnabled(true);
                    btnSetDropDeadline.setEnabled(true);
                    log.debug("Finished refreshing statuses.");
                }
            }
        };
        worker.execute();
    }

    // Helper to update label & picker for a deadline.
    private void updateDeadlineLabel(JLabel label, DatePicker picker, LocalDate deadline, String errorMsg, String type) {
        if (deadline != null) {
            label.setText(deadline.toString());
            picker.setDate(deadline);
            label.setForeground(COLOR_TEXT_PRIMARY);
        } else {
            label.setText("Not Set or Invalid");
            picker.clear();
            label.setForeground(COLOR_TEXT_SECONDARY);
            if (errorMsg != null && !errorMsg.contains("not set")) {
                JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                        "Could not load current " + type + " deadline: " + errorMsg,
                        "Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void setRegistrationDeadline() {
        final LocalDate selectedDate = datePickerRegDeadline.getDate();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid date for the deadline.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Set course registration deadline to " + selectedDate + "?",
                "Confirm Deadline Change", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        log.info("UI: Requesting to set registration deadline to: {}", selectedDate);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        datePickerRegDeadline.setEnabled(false);
        btnSetRegDeadline.setEnabled(false);

        SwingWorker<ApiResponse<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected ApiResponse<Void> doInBackground() throws Exception {
                return maintenanceApi.setRegistrationDeadline(selectedDate);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                                response.getMessage(), "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                                response.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log.error("Error setting registration deadline", e);
                    JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                            "Failed to set deadline: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    datePickerRegDeadline.setEnabled(true);
                    btnSetRegDeadline.setEnabled(true);
                    refreshStatus();
                }
            }
        };
        worker.execute();
    }

    private void setDropDeadline() {
        final LocalDate selectedDate = datePickerDropDeadline.getDate();
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid date for the deadline.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Set course drop deadline to " + selectedDate + "?",
                "Confirm Deadline Change", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        log.info("UI: Requesting to set drop deadline to: {}", selectedDate);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        datePickerDropDeadline.setEnabled(false);
        btnSetDropDeadline.setEnabled(false);

        SwingWorker<ApiResponse<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected ApiResponse<Void> doInBackground() throws Exception {
                return maintenanceApi.setDropDeadline(selectedDate);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                                response.getMessage(), "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                                response.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log.error("Error setting drop deadline", e);
                    JOptionPane.showMessageDialog(SystemSettingsPanel.this,
                            "Failed to set deadline: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    datePickerDropDeadline.setEnabled(true);
                    btnSetDropDeadline.setEnabled(true);
                    refreshStatus();
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refreshData() {
        refreshStatus();
    }
}
