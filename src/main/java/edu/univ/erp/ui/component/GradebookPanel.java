package edu.univ.erp.ui.component;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.instructor.InstructorApi;
import edu.univ.erp.api.types.GradebookRow;
import edu.univ.erp.api.types.ScoreEntryRequest;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GradebookPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(GradebookPanel.class);

    private final InstructorApi instructorApi = new InstructorApi();
    private final int sectionId;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<GradebookRow> gradebookData;
    private boolean isUpdatingTable = false;
    private JButton btnComputeFinal;

    public static final String QUIZ = "Quiz";
    public static final String MIDTERM = "Midterm";
    public static final String ENDSEM = "EndSem";
    private static final List<String> WEIGHTED_COMPONENTS = List.of(QUIZ, MIDTERM, ENDSEM);

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Color COLOR_TABLE_GRID = new Color(60, 60, 60);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    public GradebookPanel(int sectionId) {
        this.sectionId = sectionId;

        // Main layout & background
        setLayout(new MigLayout(
                "wrap 1, fill, insets 20",
                "[grow,fill]",
                "[]10[]10[grow]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Title
        JLabel mainTitle = new JLabel("Enter Grades");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        // Table
        String[] columnNames = {"Enroll ID", "Roll No", QUIZ, MIDTERM, ENDSEM, "Final Grade"};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2 && column < 5 && gradebookData != null && !gradebookData.isEmpty();
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 2 && columnIndex < 5) return Double.class; // Scores
                if (columnIndex == 5) return String.class; // Final Grade
                return super.getColumnClass(columnIndex);
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
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

        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (isUpdatingTable) return;
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (row >= 0 && column >= 2 && column < 5 &&
                            gradebookData != null && row < gradebookData.size()) {
                        onScoreEdited(row, column);
                    }
                }
            }
        });

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

        JLabel hintLabel = new JLabel("Edit scores directly in the table, then compute final grades.");
        hintLabel.setForeground(COLOR_TEXT_SECONDARY);
        hintLabel.setFont(FONT_LABEL);
        actionsPanel.add(hintLabel, "growx");

        btnComputeFinal = new JButton("Compute Final Grades");
        actionsPanel.add(btnComputeFinal, "h 32!");

        add(actionsPanel, "growx");

        btnComputeFinal.addActionListener(e -> computeFinalGrades());

        loadGradebookData();
    }

    private void loadGradebookData() {
        log.info("Loading gradebook for section {}", sectionId);
        stopCellEditing();
        isUpdatingTable = true;
        try {
            tableModel.setRowCount(0);

            ApiResponse<List<GradebookRow>> response = instructorApi.getGradebook(sectionId);

            if (response.isSuccess()) {
                this.gradebookData = response.getData();
                if (gradebookData == null || gradebookData.isEmpty()) {
                    tableModel.addRow(new Object[]{"", "No students enrolled.", null, null, null, null});
                } else {
                    for (GradebookRow row : gradebookData) {
                        tableModel.addRow(new Object[]{
                                row.enrollmentId(),
                                row.studentRollNo(),
                                row.quizScore(),
                                row.midtermScore(),
                                row.endSemScore(),
                                row.finalGrade()
                        });
                    }
                }
                log.info("Gradebook data loaded.");
            } else {
                log.error("Failed to load gradebook: {}", response.getMessage());
                this.gradebookData = null;
                tableModel.addRow(new Object[]{"", "Error loading data.", null, null, null, null});
                JOptionPane.showMessageDialog(this,
                        "Could not load gradebook: " + response.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            isUpdatingTable = false;
        }
    }

    private void onScoreEdited(int row, int column) {
        if (gradebookData == null || row >= gradebookData.size()) return;

        final String component = tableModel.getColumnName(column);
        if (!component.equals(QUIZ) && !component.equals(MIDTERM) && !component.equals(ENDSEM)) {
            log.warn("Edit detected on non-score column ({}), ignoring.", component);
            return;
        }

        GradebookRow studentDataRow = gradebookData.get(row);
        final int enrollmentId = studentDataRow.enrollmentId();
        Object valueFromTable = tableModel.getValueAt(row, column);

        Double parsedScore = null;
        boolean validInput = false;

        try {
            if (valueFromTable == null || valueFromTable.toString().trim().isEmpty()) {
                parsedScore = null;
                validInput = true;
            } else {
                parsedScore = Double.parseDouble(valueFromTable.toString());
                if (parsedScore < 0 || parsedScore > 100) {
                    throw new NumberFormatException("Score out of range (0-100).");
                }
                validInput = true;
            }
        } catch (NumberFormatException ex) {
            log.warn("Invalid score format entered: '{}' for enrollId={}, component={}",
                    valueFromTable, enrollmentId, component, ex);
            JOptionPane.showMessageDialog(this,
                    "Invalid score: '" + valueFromTable + "'.\nPlease enter a number between 0 and 100, or leave blank.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            SwingUtilities.invokeLater(this::loadGradebookData);
            return;
        }

        if (validInput) {
            final Double scoreToSave = parsedScore;
            log.debug("Score edited and validated: enrollId={}, component={}, newScore={}",
                    enrollmentId, component, scoreToSave);
            final ScoreEntryRequest request = new ScoreEntryRequest(enrollmentId, component, scoreToSave);

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingWorker<ApiResponse<Void>, Void> worker = new SwingWorker<>() {
                @Override
                protected ApiResponse<Void> doInBackground() throws Exception {
                    return instructorApi.enterScore(request);
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        ApiResponse<Void> response = get();
                        if (!response.isSuccess()) {
                            log.error("Failed to save score via API: {}", response.getMessage());
                            JOptionPane.showMessageDialog(GradebookPanel.this,
                                    "Failed to save score: " + response.getMessage() + "\nChange reverted.",
                                    "Save Error", JOptionPane.ERROR_MESSAGE);
                            loadGradebookData(); // Revert
                        } else {
                            log.info("Successfully saved score: enrollId={}, component={}, score={}",
                                    enrollmentId, component, scoreToSave);
                            updateInternalGradebookData(row, component, scoreToSave);
                        }
                    } catch (Exception e) {
                        log.error("Error saving score during background task", e);
                        JOptionPane.showMessageDialog(GradebookPanel.this,
                                "An unexpected error occurred while saving the score.\nChange reverted.",
                                "Save Error", JOptionPane.ERROR_MESSAGE);
                        loadGradebookData();
                    }
                }
            };
            worker.execute();
        }
    }

    private void stopCellEditing() {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                if (!editor.stopCellEditing()) {
                    editor.cancelCellEditing();
                }
            }
        }
    }

    private void updateInternalGradebookData(int rowIndex, String component, Double newScore) {
        if (gradebookData == null || rowIndex >= gradebookData.size()) return;
        GradebookRow oldRow = gradebookData.get(rowIndex);
        GradebookRow updatedRow = switch (component) {
            case QUIZ -> new GradebookRow(
                    oldRow.enrollmentId(), oldRow.studentId(), oldRow.studentRollNo(),
                    newScore, oldRow.midtermScore(), oldRow.endSemScore(), oldRow.finalGrade());
            case MIDTERM -> new GradebookRow(
                    oldRow.enrollmentId(), oldRow.studentId(), oldRow.studentRollNo(),
                    oldRow.quizScore(), newScore, oldRow.endSemScore(), oldRow.finalGrade());
            case ENDSEM -> new GradebookRow(
                    oldRow.enrollmentId(), oldRow.studentId(), oldRow.studentRollNo(),
                    oldRow.quizScore(), oldRow.midtermScore(), newScore, oldRow.finalGrade());
            default -> oldRow;
        };
        gradebookData.set(rowIndex, updatedRow);
    }

    // Compute final grades
    private void computeFinalGrades() {
        stopCellEditing();

        String boundaryInput = JOptionPane.showInputDialog(
                this,
                "Enter comma-separated minimum scores for grades A+, A, B, C, D\n" +
                        "(e.g., 90, 80, 70, 60, 50)\n" +
                        "Scores must be 0-100 and strictly descending.",
                "Enter Grade Boundaries",
                JOptionPane.PLAIN_MESSAGE
        );

        if (boundaryInput == null) {
            log.debug("User cancelled grade boundary input.");
            return;
        }

        String[] boundaryStrings = boundaryInput.split(",");
        List<Double> boundaries = new ArrayList<>();

        if (boundaryStrings.length != 5) {
            showValidationError("Invalid input: Expected exactly 5 comma-separated numbers for A+, A, B, C, D boundaries.");
            return;
        }

        try {
            for (String boundaryStr : boundaryStrings) {
                String trimmedStr = boundaryStr.trim();
                if (trimmedStr.isEmpty())
                    throw new NumberFormatException("Boundary value cannot be empty.");
                double boundary = Double.parseDouble(trimmedStr);
                if (boundary < 0 || boundary > 100)
                    throw new IllegalArgumentException("Boundaries must be between 0 and 100.");
                boundaries.add(boundary);
            }
        } catch (IllegalArgumentException e) {
            showValidationError("Invalid Boundary Input: " + e.getMessage());
            return;
        }

        for (int i = 0; i < boundaries.size() - 1; i++) {
            if (boundaries.get(i) <= boundaries.get(i + 1)) {
                showValidationError("Invalid Boundaries: Boundaries must be strictly descending (A+ > A > B > C > D).");
                return;
            }
        }

        log.info("UI: Requesting final grade computation for section {} with boundaries: {}", sectionId, boundaries);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnComputeFinal.setEnabled(false);

        final List<Double> finalBoundaries = boundaries;

        SwingWorker<ApiResponse<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected ApiResponse<Void> doInBackground() throws Exception {
                return instructorApi.computeFinalGrades(sectionId, finalBoundaries);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(GradebookPanel.this,
                                response.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadGradebookData();
                    } else {
                        JOptionPane.showMessageDialog(GradebookPanel.this,
                                response.getMessage(), "Computation Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log.error("Error retrieving result from computeFinalGrades worker", e);
                    JOptionPane.showMessageDialog(GradebookPanel.this,
                            "An unexpected error occurred: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    btnComputeFinal.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void showValidationError(String message) {
        log.warn("Input validation failed: {}", message);
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refreshData() {
        loadGradebookData();
    }
}
