package edu.univ.erp.ui.component;

import edu.univ.erp.api.admin.AdminApi;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditCoursesPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(EditCoursesPanel.class);

    private final AdminApi adminApi = new AdminApi();

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_SECTION_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    private JComboBox<Course> cmbCourses;
    private JComboBox<SectionDisplayItem> cmbSections;

    private JLabel lblCourseHeader;
    private JLabel lblSectionHeader;

    private JPanel pnlCourseForm;
    private JTextField txtCourseCode; // read-only
    private JTextField txtCourseTitle;
    private JSpinner spnCourseCredits;
    private JButton btnUpdateCourse;

    private JPanel pnlSectionForm;
    private JLabel lblSectionId;
    private JComboBox<Instructor> cmbInstructors;
    private JTextField txtDayTime;
    private JTextField txtRoom;
    private JSpinner spnCapacity;
    private JTextField txtSemester;
    private JSpinner spnYear;
    private JButton btnUpdateSection;

    private Map<Integer, String> instructorNameMap = new HashMap<>();

    public EditCoursesPanel() {
        setLayout(new MigLayout(
                "wrap 1, fillx, insets 20",
                "[grow,fill]",
                "[]15[]15[]15[]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Main title
        JLabel mainTitle = new JLabel("Edit Courses and Sections");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        initSelectors();
        initCourseForm();
        initSectionForm();

        setCourseSectionVisible(false);
        setSectionEditVisible(false);

        loadCourses();
    }

    private void initSelectors() {
        JPanel pnlSelectors = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,110:pref]15[grow,fill]",
                "[]8[]"
        ));
        pnlSelectors.setOpaque(false);

        // Select Course
        pnlSelectors.add(createLabel("Select Course:"));
        cmbCourses = new JComboBox<>();
        cmbCourses.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Course c) {
                    setText(String.format("%s (%s)", c.code(), c.title()));
                } else if (value == null && index == -1) {
                    setText("-- Select Course --");
                }
                return this;
            }
        });
        pnlSelectors.add(cmbCourses, "growx");

        // Select Section
        pnlSelectors.add(createLabel("Select Section:"));
        cmbSections = new JComboBox<>();
        cmbSections.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SectionDisplayItem sdi) {
                    setText(sdi.toString());
                } else if (value == null && index == -1) {
                    setText("-- Select Section --");
                } else if (value == null) {
                    setText("");
                }
                return this;
            }
        });
        pnlSelectors.add(cmbSections, "growx");

        add(pnlSelectors, "growx");

        // Selection listeners
        cmbCourses.addActionListener(e -> onCourseSelected());
        cmbSections.addActionListener(e -> onSectionSelected());
    }

    private void initCourseForm() {
        // Header label (we control visibility separately)
        lblCourseHeader = new JLabel("Edit Course");
        lblCourseHeader.setFont(FONT_SECTION_TITLE);
        lblCourseHeader.setForeground(COLOR_TEXT_PRIMARY);
        add(lblCourseHeader, "growx, gaptop 10");   // visibility toggled later

        pnlCourseForm = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,110:pref]15[grow,fill]",
                "[]8[]8[]12[]"
        ));
        pnlCourseForm.setOpaque(false);

        pnlCourseForm.add(createLabel("Course Code:"));
        txtCourseCode = new JTextField();
        txtCourseCode.setEditable(false);
        pnlCourseForm.add(txtCourseCode, "growx");

        pnlCourseForm.add(createLabel("Title:"));
        txtCourseTitle = new JTextField();
        pnlCourseForm.add(txtCourseTitle, "growx");

        pnlCourseForm.add(createLabel("Credits:"));
        spnCourseCredits = new JSpinner(new SpinnerNumberModel(4, null, null, 1));
        pnlCourseForm.add(spnCourseCredits, "left");

        btnUpdateCourse = new JButton("Update Course");
        pnlCourseForm.add(btnUpdateCourse, "span, growx, h 32!");
        btnUpdateCourse.addActionListener(e -> updateCourseAction());

        add(pnlCourseForm, "growx");
    }

    private void initSectionForm() {
        // Header label (we control visibility separately)
        lblSectionHeader = new JLabel("Edit Section");
        lblSectionHeader.setFont(FONT_SECTION_TITLE);
        lblSectionHeader.setForeground(COLOR_TEXT_PRIMARY);
        add(lblSectionHeader, "growx, gaptop 15");  // visibility toggled later

        pnlSectionForm = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,110:pref]15[grow,fill]",
                "[]8[]8[]8[]8[]8[]8[]12[]"
        ));
        pnlSectionForm.setOpaque(false);

        pnlSectionForm.add(createLabel("Section ID:"));
        lblSectionId = new JLabel();
        lblSectionId.setForeground(COLOR_TEXT_SECONDARY);
        pnlSectionForm.add(lblSectionId, "growx");

        pnlSectionForm.add(createLabel("Instructor:"));
        cmbInstructors = new JComboBox<>();
        cmbInstructors.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Instructor i) {
                    setText(i.name() + " (" + i.department() + ")");
                } else if (value == null) {
                    setText("-- Unassigned --");
                }
                return this;
            }
        });
        pnlSectionForm.add(cmbInstructors, "growx");

        pnlSectionForm.add(createLabel("Day / Time:"));
        txtDayTime = new JTextField();
        pnlSectionForm.add(txtDayTime, "growx");

        pnlSectionForm.add(createLabel("Room:"));
        txtRoom = new JTextField();
        pnlSectionForm.add(txtRoom, "growx");

        pnlSectionForm.add(createLabel("Capacity:"));
        spnCapacity = new JSpinner(new SpinnerNumberModel(30, null, null, 1));
        pnlSectionForm.add(spnCapacity, "left");

        pnlSectionForm.add(createLabel("Semester:"));
        txtSemester = new JTextField();
        pnlSectionForm.add(txtSemester, "growx");

        pnlSectionForm.add(createLabel("Year:"));
        spnYear = new JSpinner(new SpinnerNumberModel(2025, null, null, 1));
        pnlSectionForm.add(spnYear, "left");

        btnUpdateSection = new JButton("Update Section");
        pnlSectionForm.add(btnUpdateSection, "span, growx, h 32!");
        btnUpdateSection.addActionListener(e -> updateSectionAction());

        add(pnlSectionForm, "growx");
    }

    // Small helper for labels
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT_SECONDARY);
        label.setFont(FONT_LABEL);
        return label;
    }

    // Visibility helpers
    private void setCourseSectionVisible(boolean visible) {
        lblCourseHeader.setVisible(visible);
        pnlCourseForm.setVisible(visible);
    }

    private void setSectionEditVisible(boolean visible) {
        lblSectionHeader.setVisible(visible);
        pnlSectionForm.setVisible(visible);
    }

    // Action / helper methods
    private void loadCourses() {
        ApiResponse<List<Course>> resp = adminApi.getAllCourses();
        cmbCourses.removeAllItems();
        if (resp.isSuccess() && resp.getData() != null) {
            for (Course c : resp.getData()) {
                cmbCourses.addItem(c);
            }
            cmbCourses.setSelectedIndex(-1);
        } else {
            log.error("Failed to load courses: {}", resp.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not load courses: " + resp.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // Hide everything when reloading
        setCourseSectionVisible(false);
        setSectionEditVisible(false);
        cmbSections.removeAllItems();
    }

    private void onCourseSelected() {
        Course selected = (Course) cmbCourses.getSelectedItem();
        if (selected == null) {
            // No course -> hide course + section editors
            setCourseSectionVisible(false);
            setSectionEditVisible(false);
            cmbSections.removeAllItems();
            revalidate();
            repaint();
            return;
        }

        // Show and populate course form
        txtCourseCode.setText(selected.code());
        txtCourseTitle.setText(selected.title());
        spnCourseCredits.setValue(selected.credits());
        setCourseSectionVisible(true);

        // Load sections for this course
        loadSectionsForCourse(selected.courseId(), selected.code());

        // Hide section editor until a section is chosen
        setSectionEditVisible(false);

        revalidate();
        repaint();
    }

    // Load sections for the given course and populate with SectionDisplayItem.
    private void loadSectionsForCourse(int courseId, String courseCode) {
        // Load instructors map
        instructorNameMap.clear();
        ApiResponse<List<Instructor>> iResp = adminApi.getAllInstructors();
        if (iResp.isSuccess() && iResp.getData() != null) {
            for (Instructor ins : iResp.getData()) {
                instructorNameMap.put(ins.userId(), ins.name());
            }
        } else {
            log.warn("Failed to load instructors for display: {}", iResp.getMessage());
        }

        ApiResponse<List<Section>> resp = adminApi.getSectionsByCourse(courseId);
        cmbSections.removeAllItems();
        if (resp.isSuccess() && resp.getData() != null) {
            for (Section s : resp.getData()) {
                String instrName = null;
                if (s.instructorId() != null) {
                    instrName = instructorNameMap.get(s.instructorId());
                }
                SectionDisplayItem item = new SectionDisplayItem(s, courseCode, instrName);
                cmbSections.addItem(item);
            }
            cmbSections.setSelectedIndex(-1);
        } else {
            log.error("Failed to load sections for course {}: {}", courseId, resp.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not load sections: " + resp.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSectionSelected() {
        SectionDisplayItem sdi = (SectionDisplayItem) cmbSections.getSelectedItem();
        if (sdi == null) {
            // No section -> hide section editor only
            setSectionEditVisible(false);
            revalidate();
            repaint();
            return;
        }

        Section selected = sdi.section();

        // Populate section form
        lblSectionId.setText(String.valueOf(selected.sectionId()));
        txtDayTime.setText(Objects.toString(selected.dayTime(), ""));
        txtRoom.setText(Objects.toString(selected.room(), ""));
        spnCapacity.setValue(selected.capacity());
        txtSemester.setText(Objects.toString(selected.semester(), ""));
        spnYear.setValue(selected.year());

        // Load all instructors into the instructor combo box and select the current one
        loadAllInstructorsAndSelect(selected.instructorId());

        setSectionEditVisible(true);
        revalidate();
        repaint();
    }

    private void loadAllInstructorsAndSelect(Integer selectedInstructorId) {
        ApiResponse<List<Instructor>> resp = adminApi.getAllInstructors();
        cmbInstructors.removeAllItems();
        cmbInstructors.addItem(null);
        if (resp.isSuccess() && resp.getData() != null) {
            int indexToSelect = 0;
            int idx = 1;
            for (Instructor ins : resp.getData()) {
                cmbInstructors.addItem(ins);
                if (selectedInstructorId != null && selectedInstructorId.equals(ins.userId())) {
                    indexToSelect = idx;
                }
                idx++;
            }
            cmbInstructors.setSelectedIndex(indexToSelect);
        } else {
            log.error("Failed to load instructors: {}", resp.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not load instructors: " + resp.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Update actions
    private void updateCourseAction() {
        Course selectedCourse = (Course) cmbCourses.getSelectedItem();
        if (selectedCourse == null) {
            JOptionPane.showMessageDialog(this,
                    "No course selected.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newTitle = txtCourseTitle.getText().trim();
        int newCredits = (Integer) spnCourseCredits.getValue();

        if (newTitle.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Course title cannot be empty.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newCredits <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Credits must be positive.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        ApiResponse<Void> resp = adminApi.editCourse(selectedCourse.courseId(), newTitle, newCredits);
        if (resp.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Course updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            int updatedId = selectedCourse.courseId();
            loadCourses();
            // Re-select same course
            for (int i = 0; i < cmbCourses.getItemCount(); i++) {
                Course c = cmbCourses.getItemAt(i);
                if (c != null && c.courseId() == updatedId) {
                    cmbCourses.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    resp.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSectionAction() {
        SectionDisplayItem sdi = (SectionDisplayItem) cmbSections.getSelectedItem();
        if (sdi == null) {
            JOptionPane.showMessageDialog(this,
                    "No section selected.",
                    "Input Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Section selectedSection = sdi.section();

        Integer instructorId = null;
        Instructor selIns = (Instructor) cmbInstructors.getSelectedItem();
        if (selIns != null) instructorId = selIns.userId();

        String dayTime = txtDayTime.getText().trim();
        String room = txtRoom.getText().trim();
        int capacity = (Integer) spnCapacity.getValue();
        String semester = txtSemester.getText().trim();
        int year = (Integer) spnYear.getValue();

        if (semester.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Semester cannot be empty.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (capacity <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Capacity must be a positive number.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        ApiResponse<Void> resp = adminApi.editSection(selectedSection.sectionId(), instructorId, dayTime, room, capacity, semester, year);

        if (resp.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Section updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Reload sections for the currently selected course and re-select updated section
            Course selCourse = (Course) cmbCourses.getSelectedItem();
            if (selCourse != null) {
                int courseId = selCourse.courseId();
                int updatedSectionId = selectedSection.sectionId();
                loadSectionsForCourse(courseId, selCourse.code());
                for (int i = 0; i < cmbSections.getItemCount(); i++) {
                    SectionDisplayItem it = cmbSections.getItemAt(i);
                    if (it != null && it.section().sectionId() == updatedSectionId) {
                        cmbSections.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    resp.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Wrapper used to populate the sections box with display-ready info.
    private static class SectionDisplayItem {
        private final Section section;
        private final String courseCode;
        private final String instructorName;
        private final String dayTime;
        private final String room;

        SectionDisplayItem(Section s, String courseCode, String instructorName) {
            this.section = s;
            this.courseCode = courseCode != null ? courseCode : "";
            this.instructorName = instructorName != null ? instructorName : "Unassigned";
            this.dayTime = s.dayTime() != null ? s.dayTime() : "";
            this.room = s.room() != null ? s.room() : "";
        }

        public Section section() {
            return section;
        }

        @Override
        public String toString() {
            return String.join(" | ",
                    courseCode,
                    instructorName,
                    dayTime,
                    room
            );
        }
    }

    @Override
    public void refreshData() {
        loadCourses();
    }
}
