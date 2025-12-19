package edu.univ.erp.ui.component;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

// A simple modal dialog to show progress messages.
public class ProgressDialog extends JDialog {

    private JTextArea textArea;
    private JProgressBar progressBar;

    public ProgressDialog(Frame owner, String title) {
        super(owner, title, true); // Modal
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setLayout(new MigLayout("fill, insets 10", "[grow]", "[grow]10[]"));
        setSize(400, 300);
        setLocationRelativeTo(owner);

        textArea = new JTextArea("Starting process...\n");
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        add(new JScrollPane(textArea), "grow, wrap");

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        add(progressBar, "growx");
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            // Auto-scroll to the bottom
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    public void complete(boolean success) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(success ? 100 : 0);
            if (!success) {
                progressBar.setForeground(Color.RED);
            }
            setTitle(getTitle() + (success ? " - Completed" : " - Failed"));
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        });
    }
}