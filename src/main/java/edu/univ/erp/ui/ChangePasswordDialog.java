package edu.univ.erp.ui;

import edu.univ.erp.api.auth.AuthApi;
import edu.univ.erp.api.common.ApiResponse;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class ChangePasswordDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordDialog.class);
    private final AuthApi authApi = new AuthApi();

    private JPasswordField txtOldPassword;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnChange;
    private JButton btnCancel;

    public ChangePasswordDialog(Frame owner) {
        super(owner, "Change Password", true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new MigLayout("wrap 2, fillx", "[right]10[grow,fill]", "[]10[]10[]20[]"));

        add(new JLabel("Current Password:"));
        txtOldPassword = new JPasswordField();
        add(txtOldPassword, "growx");

        add(new JLabel("New Password:"));
        txtNewPassword = new JPasswordField();
        add(txtNewPassword, "growx");

        add(new JLabel("Confirm New Password:"));
        txtConfirmPassword = new JPasswordField();
        add(txtConfirmPassword, "growx");

        btnChange = new JButton("Change Password");
        btnCancel = new JButton("Cancel");

        add(btnCancel, "split 2, skip 1, align right, sg buttons");
        add(btnChange, "sg buttons");

        btnChange.addActionListener(e -> performPasswordChange());
        btnCancel.addActionListener(e -> dispose());
        txtConfirmPassword.addActionListener(e -> performPasswordChange());

        pack();
        setLocationRelativeTo(owner);
    }

    private void performPasswordChange() {
        char[] oldPass = txtOldPassword.getPassword();
        char[] newPass = txtNewPassword.getPassword();
        char[] confirmPass = txtConfirmPassword.getPassword();

        if (oldPass.length == 0 || newPass.length == 0 || confirmPass.length == 0) {
            showError("All password fields are required.");
            return;
        }
        if (!Arrays.equals(newPass, confirmPass)) {
            showError("New passwords do not match.");
            txtNewPassword.setText("");
            txtConfirmPassword.setText("");
            txtNewPassword.requestFocusInWindow();
            return;
        }
        if (Arrays.equals(oldPass, newPass)) {
            showError("New password cannot be the same as the old password.");
            txtNewPassword.setText("");
            txtConfirmPassword.setText("");
            txtNewPassword.requestFocusInWindow();
            return;
        }
        if (newPass.length < 6) {
            showError("New password must be at least 6 characters long.");
            txtNewPassword.requestFocusInWindow();
            return;
        }


        String oldPasswordStr = new String(oldPass);
        String newPasswordStr = new String(newPass);

        // Clear password arrays from memory ASAP (basic security measure)
        Arrays.fill(oldPass, ' ');
        Arrays.fill(newPass, ' ');
        Arrays.fill(confirmPass, ' ');

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnChange.setEnabled(false);
        btnCancel.setEnabled(false);

        ApiResponse<Void> response = authApi.changePassword(oldPasswordStr, newPasswordStr);

        setCursor(Cursor.getDefaultCursor());
        btnChange.setEnabled(true);
        btnCancel.setEnabled(true);

        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose(); // Close dialog on success
        } else {
            showError(response.getMessage());
            // Clear only old password on failure
            txtOldPassword.setText("");
            txtOldPassword.requestFocusInWindow();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Password Change Error", JOptionPane.ERROR_MESSAGE);
    }
}