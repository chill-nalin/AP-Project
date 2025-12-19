package edu.univ.erp.ui.component;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.reports.ReportsApi;
import edu.univ.erp.api.student.StudentApi;
import edu.univ.erp.api.types.GradeRow;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.util.List;

// A JPanel that displays the student's grades in a JTable.
public class MyGradesPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(MyGradesPanel.class);

    private final StudentApi studentApi = new StudentApi();
    private final ReportsApi reportsApi = new ReportsApi();
    private final User currentUser;
    private JTable table;
    private DefaultTableModel tableModel;

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Color COLOR_TABLE_GRID = new Color(60, 60, 60);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_SECTION_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    public MyGradesPanel(User user) {
        this.currentUser = user;

        setLayout(new MigLayout(
                "wrap 1, fill, insets 20",
                "[grow,fill]",
                "[]10[]10[grow]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Title
        JLabel mainTitle = new JLabel("My Grades");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        // Subtitle
        JLabel lblSubtitle = new JLabel("Current and past course performance");
        lblSubtitle.setFont(FONT_SECTION_TITLE);
        lblSubtitle.setForeground(COLOR_TEXT_PRIMARY);
        add(lblSubtitle, "growx, wrap");

        // Table
        String[] columnNames = {"Course Code", "Course Title", "Component", "Score", "Final Grade"};
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
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
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

        JLabel hintLabel = new JLabel("Download your full transcript as a CSV file.");
        hintLabel.setForeground(COLOR_TEXT_SECONDARY);
        hintLabel.setFont(FONT_LABEL);
        actionsPanel.add(hintLabel, "growx");

        JButton btnDownload = new JButton("Download Transcript (CSV)");
        actionsPanel.add(btnDownload, "h 32!");

        add(actionsPanel, "growx");

        btnDownload.addActionListener(e -> downloadTranscript());

        loadGradeData();
    }

    // Data loading
    private void loadGradeData() {
        log.info("Loading grades for student {}", currentUser.userId());
        tableModel.setRowCount(0);

        ApiResponse<List<GradeRow>> response = studentApi.getMyGrades(currentUser.userId());

        if (response.isSuccess()) {
            List<GradeRow> grades = response.getData();
            if (grades.isEmpty()) {
                tableModel.addRow(new Object[]{"", "No grades available yet.", "", "", ""});
            } else {
                for (GradeRow row : grades) {
                    tableModel.addRow(new Object[]{
                            row.courseCode(),
                            row.courseTitle(),
                            row.component(),
                            row.scoreDisplay(),
                            row.finalGrade()
                    });
                }
            }
            log.info("Grade data loaded into table.");
        } else {
            log.error("Failed to load grades: {}", response.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load your grades: " + response.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Download transcript
    private void downloadTranscript() {
        log.debug("Download Transcript button clicked by student {}", currentUser.userId());

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Transcript As");
        fileChooser.setSelectedFile(new File("Transcript_" + currentUser.username() + ".csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
            }
            if (fileToSave.exists()) {
                int overwriteChoice = JOptionPane.showConfirmDialog(
                        this,
                        "File already exists. Overwrite?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION
                );
                if (overwriteChoice != JOptionPane.YES_OPTION) {
                    log.info("User cancelled overwrite.");
                    return;
                }
            }

            log.info("User chose to save transcript to: {}", fileToSave.getAbsolutePath());
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Use ReportsApi to generate transcript
            ApiResponse<Void> response =
                    reportsApi.downloadStudentTranscript(currentUser.userId(), fileToSave);

            setCursor(Cursor.getDefaultCursor());

            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(
                        this,
                        response.getMessage(),
                        "Transcript Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Error saving transcript:\n" + response.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        } else {
            log.info("User cancelled transcript save dialog.");
        }
    }

    @Override
    public void refreshData() {
        loadGradeData();
    }
}
