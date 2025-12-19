package edu.univ.erp.ui.component;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.api.admin.AdminApi;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.types.UserCreationRequest;
import edu.univ.erp.domain.Role;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class UserManagementPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(UserManagementPanel.class);
    private final AdminApi adminApi = new AdminApi();

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<Role> cmbRole;
    private JTextField txtName; // Instructor
    private JTextField txtDepartment; // Instructor
    private JTextField txtRollNo; // Student
    private JTextField txtProgram; // Student
    private JSpinner spnYear; // Student
    private JButton btnCreateUser;

    private JPanel studentPanel;
    private JPanel instructorPanel;

    public UserManagementPanel() {
        setLayout(new MigLayout("wrap 2, fillx, insets 20", "[right, 90:pref]15[grow,fill]", "[]20[]10[]10[]15[]15[]25[]"));

        setBackground(COLOR_BACKGROUND);

        // Title
        JLabel lblTitle = new JLabel("Create New User");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(lblTitle, "span 2, wrap, gapbottom 15");

        // Core Fields
        add(createLabel("Username:"));
        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "stu1");
        add(txtUsername, "growx");

        add(createLabel("Password:"));
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "••••••••");
        add(txtPassword, "growx");

        add(createLabel("Role:"));
        cmbRole = new JComboBox<>(Role.values());
        add(cmbRole, "growx");
        cmbRole.addActionListener(e -> updateRoleSpecificFields());

        // Student Specific Fields
        studentPanel = new JPanel(new MigLayout("wrap 2, insets 0, fillx", "[right, 90:pref]15[grow,fill]"));
        studentPanel.setOpaque(false);
        studentPanel.add(createLabel("Roll No:"));
        txtRollNo = new JTextField();
        txtRollNo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "2024001");
        studentPanel.add(txtRollNo, "growx");
        studentPanel.add(createLabel("Program:"));
        txtProgram = new JTextField();
        txtProgram.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "B.Tech CSE");
        studentPanel.add(txtProgram, "growx");
        studentPanel.add(createLabel("Year:"));
        spnYear = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        studentPanel.add(spnYear, "align left");
        add(studentPanel, "span 2, growx, hidemode 3");

        // Instructor Specific Fields
        instructorPanel = new JPanel(new MigLayout("wrap 2, insets 0, fillx", "[right, 90:pref]15[grow,fill]"));
        instructorPanel.setOpaque(false); // Make transparent
        instructorPanel.add(createLabel("Name:"));
        txtName = new JTextField();
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g., Dr. Emily Carter");
        instructorPanel.add(txtName, "growx");
        instructorPanel.add(createLabel("Department:"));
        txtDepartment = new JTextField();
        txtDepartment.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g., Computer Science");
        instructorPanel.add(txtDepartment, "growx");
        add(instructorPanel, "span 2, growx, hidemode 3");

        btnCreateUser = new JButton("Create User");
        add(btnCreateUser, "skip 1, span, growx, h 35!");

        btnCreateUser.addActionListener(e -> createUser());

        updateRoleSpecificFields();
    }

    // Helper to create styled JLabels
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT_SECONDARY);
        label.setFont(FONT_LABEL);
        return label;
    }

    private void updateRoleSpecificFields() {
        Role selectedRole = (Role) cmbRole.getSelectedItem();
        studentPanel.setVisible(selectedRole == Role.Student);
        instructorPanel.setVisible(selectedRole == Role.Instructor);

        // Re-validate the panel layout
        revalidate();
        // Repaint to ensure correct rendering
        repaint();
    }

    private void createUser() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        Role role = (Role) cmbRole.getSelectedItem();
        String name = txtName.getText().trim();
        String dept = txtDepartment.getText().trim();
        String rollNo = txtRollNo.getText().trim();
        String program = txtProgram.getText().trim();
        int year = (Integer) spnYear.getValue();

        // Basic client-side validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (role == Role.Student && rollNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Roll Number is required for students.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (role == Role.Instructor && name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required for instructors.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserCreationRequest request = new UserCreationRequest(
                username, password, role, name, rollNo, program, year, dept
        );

        // Disable button during API call
        btnCreateUser.setEnabled(false);
        btnCreateUser.setText("Creating...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<ApiResponse<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected ApiResponse<Void> doInBackground() throws Exception {
                return adminApi.createUser(request);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(UserManagementPanel.this, response.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        txtUsername.setText("");
                        txtPassword.setText("");
                        txtName.setText("");
                        txtDepartment.setText("");
                        txtRollNo.setText("");
                        txtProgram.setText("");
                        spnYear.setValue(1);
                        cmbRole.setSelectedIndex(0);
                    } else {
                        JOptionPane.showMessageDialog(UserManagementPanel.this, response.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log.error("Failed to create user", e);
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "An unexpected error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Re-enable button and reset cursor
                    btnCreateUser.setEnabled(true);
                    btnCreateUser.setText("Create User");
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    @Override
    public void refreshData() {
        updateRoleSpecificFields();
    }
}