package edu.univ.erp.ui.component;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.api.admin.AdminApi;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.data.CourseRepository;
import edu.univ.erp.data.SectionRepository;
import edu.univ.erp.data.SettingsRepository;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CourseManagementPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(CourseManagementPanel.class);
    private final AdminApi adminApi = new AdminApi();
    private final SectionRepository sectionRepo = new SectionRepository();
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final CourseRepository courseRepo = new CourseRepository();

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_SECTION_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);

    private JTextField txtCourseCode;
    private JTextField txtCourseTitle;
    private JSpinner spnCredits;
    private JComboBox<Course> cmbCourses;
    private JComboBox<Instructor> cmbInstructors;
    private JTextField txtDayTime;
    private JTextField txtRoom;
    private JSpinner spnCapacity;
    private JTextField txtSemester;
    private JSpinner spnSectionYear;
    private JComboBox<SectionDisplay> cmbSections;
    private DefaultComboBoxModel<Course> courseModel;
    private DefaultComboBoxModel<Instructor> instructorModel;
    private DefaultComboBoxModel<SectionDisplay> sectionModel;

    private record SectionDisplay(int sectionId, String displayText) {
        @Override
        public String toString() {
            return displayText;
        }

        public static Comparator<SectionDisplay> CODE_COMPARATOR =
                Comparator.comparing(sd -> sd.displayText().split(" ")[0]);
    }

    public CourseManagementPanel() {
        setLayout(new MigLayout(
                "wrap 1, fillx, insets 20",
                "[grow,fill]",
                "[]15[]15[]15[]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Main title
        JLabel mainTitle = new JLabel("Manage Courses and Sections");
        mainTitle.setFont(FONT_TITLE);
        mainTitle.setForeground(COLOR_TEXT_PRIMARY);
        add(mainTitle, "growx, wrap");

        JLabel lblCourseSection = new JLabel("Create New Course");
        lblCourseSection.setFont(FONT_SECTION_TITLE);
        lblCourseSection.setForeground(COLOR_TEXT_PRIMARY);
        add(lblCourseSection, "growx, gaptop 5");

        JPanel coursePanel = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,90:pref]15[grow,fill]",
                "[]10[]10[]15[]"
        ));
        coursePanel.setOpaque(false);

        coursePanel.add(createLabel("Code:"));
        txtCourseCode = new JTextField();
        txtCourseCode.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "CSE201");
        coursePanel.add(txtCourseCode, "growx");

        coursePanel.add(createLabel("Title:"));
        txtCourseTitle = new JTextField();
        txtCourseTitle.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Intro to Programming");
        coursePanel.add(txtCourseTitle, "growx");

        coursePanel.add(createLabel("Credits:"));
        spnCredits = new JSpinner(new SpinnerNumberModel(4, null, null, 1));
        coursePanel.add(spnCredits, "left");

        JButton btnCreateCourse = new JButton("Create Course");
        coursePanel.add(btnCreateCourse, "skip 1, span, growx, h 32!");
        btnCreateCourse.addActionListener(e -> createCourse());

        add(coursePanel, "growx, wrap");

        JLabel lblSectionSection = new JLabel("Create New Section");
        lblSectionSection.setFont(FONT_SECTION_TITLE);
        lblSectionSection.setForeground(COLOR_TEXT_PRIMARY);
        add(lblSectionSection, "growx");

        JPanel sectionPanel = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,130:pref]15[grow,fill]",
                "[]10[]10[]10[]10[]10[]10[]15[]"
        ));
        sectionPanel.setOpaque(false);

        sectionPanel.add(createLabel("For Course:"));
        courseModel = new DefaultComboBoxModel<>();
        cmbCourses = new JComboBox<>(courseModel);
        cmbCourses.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Course course) {
                    setText(course.code() + " - " + course.title());
                } else if (value == null && index == -1) {
                    setText("-- Select Course --");
                }
                return this;
            }
        });
        sectionPanel.add(cmbCourses, "growx");

        sectionPanel.add(createLabel("Instructor (Optional):"));
        instructorModel = new DefaultComboBoxModel<>();
        cmbInstructors = new JComboBox<>(instructorModel);
        cmbInstructors.setRenderer(new InstructorRenderer());
        sectionPanel.add(cmbInstructors, "growx");

        sectionPanel.add(createLabel("Day / Time:"));
        txtDayTime = new JTextField();
        txtDayTime.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mon 10:00–11:30");
        sectionPanel.add(txtDayTime, "growx");

        sectionPanel.add(createLabel("Room:"));
        txtRoom = new JTextField();
        txtRoom.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "C-203");
        sectionPanel.add(txtRoom, "growx");

        sectionPanel.add(createLabel("Capacity:"));
        spnCapacity = new JSpinner(new SpinnerNumberModel(30, null, null, 10));
        sectionPanel.add(spnCapacity, "left");

        sectionPanel.add(createLabel("Semester:"));
        txtSemester = new JTextField();
        sectionPanel.add(txtSemester, "growx");

        sectionPanel.add(createLabel("Year:"));
        spnSectionYear = new JSpinner(new SpinnerNumberModel(2025, null, null, 1));
        sectionPanel.add(spnSectionYear, "left");

        JButton btnCreateSection = new JButton("Create Section");
        sectionPanel.add(btnCreateSection, "skip 1, span, growx, h 32!");
        btnCreateSection.addActionListener(e -> createSection());

        add(sectionPanel, "growx, wrap");

        JLabel lblAssignSection = new JLabel("Manage Section Assignment / Deletion");
        lblAssignSection.setFont(FONT_SECTION_TITLE);
        lblAssignSection.setForeground(COLOR_TEXT_PRIMARY);
        add(lblAssignSection, "growx");

        JPanel assignPanel = new JPanel(new MigLayout(
                "wrap 2, fillx, insets 10 0 5 0",
                "[right,130:pref]15[grow,fill]",
                "[]10[]15[]"
        ));
        assignPanel.setOpaque(false);

        assignPanel.add(createLabel("Section:"));
        sectionModel = new DefaultComboBoxModel<>();
        cmbSections = new JComboBox<>(sectionModel);
        assignPanel.add(cmbSections, "growx");

        assignPanel.add(createLabel("Assign Instructor:"));
        JComboBox<Instructor> cmbAssignInstructor = new JComboBox<>(instructorModel);
        cmbAssignInstructor.setRenderer(new InstructorRenderer());
        assignPanel.add(cmbAssignInstructor, "growx");

        JButton btnAssign = new JButton("Assign / Unassign Instructor");
        JButton btnDeleteSection = new JButton("Delete Selected Section");
        btnDeleteSection.setForeground(Color.RED.darker());

        assignPanel.add(btnDeleteSection, "split 2, skip 1, align right, sg buttons, h 30!");
        assignPanel.add(btnAssign, "sg buttons, h 30!");

        btnAssign.addActionListener(e -> assignInstructor(cmbAssignInstructor));
        btnDeleteSection.addActionListener(e -> deleteSection());

        add(assignPanel, "growx");

        loadInitialData();
    }

    // Small helper for labels
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT_SECONDARY);
        label.setFont(FONT_LABEL);
        return label;
    }

    private void loadInitialData() {
        log.info("Loading/Refreshing initial data for Course Management Panel...");
        Object selectedCourse = courseModel.getSelectedItem();
        Object selectedInstructor = instructorModel.getSelectedItem();
        Object selectedSection = sectionModel.getSelectedItem();

        ApiResponse<List<Course>> courseResponse = adminApi.getAllCourses();
        courseModel.removeAllElements();
        if (courseResponse.isSuccess()) {
            courseModel.addAll(courseResponse.getData());
            courseModel.setSelectedItem(selectedCourse);
        } else {
            log.error("Failed to load courses for dropdown: {}", courseResponse.getMessage());
        }

        ApiResponse<List<Instructor>> instructorResponse = adminApi.getAllInstructors();
        instructorModel.removeAllElements();
        instructorModel.addElement(null);
        if (instructorResponse.isSuccess()) {
            instructorModel.addAll(instructorResponse.getData());
            instructorModel.setSelectedItem(selectedInstructor);
        } else {
            log.error("Failed to load instructors for dropdown: {}", instructorResponse.getMessage());
        }

        txtSemester.setText(settingsRepo.getCurrentSemester());
        spnSectionYear.setValue(settingsRepo.getCurrentYear());

        loadSectionsForAssignment(selectedSection);
        log.info("Finished loading/refreshing data.");
    }

    private void loadSectionsForAssignment(Object previousSelection) {
        sectionModel.removeAllElements();
        String semester = settingsRepo.getCurrentSemester();
        int year = settingsRepo.getCurrentYear();
        List<Section> sections = sectionRepo.findAllBySemesterAndYear(semester, year);
        List<SectionDisplay> displayList = new ArrayList<>();

        Map<Integer, String> courseCodeMap = courseRepo.findAll().stream()
                .collect(Collectors.toMap(Course::courseId, Course::code));

        // Fetch all instructors once and map by userId
        Map<Integer, String> instructorNameMap = adminApi.getAllInstructors()
                .getData()
                .stream()
                .collect(Collectors.toMap(Instructor::userId, Instructor::name));

        for (Section s : sections) {
            String courseCode = courseCodeMap.getOrDefault(s.courseId(), "???");

            // Instructor name or Unassigned
            String instructorName = "Unassigned";
            if (s.instructorId() != null && instructorNameMap.containsKey(s.instructorId())) {
                instructorName = instructorNameMap.get(s.instructorId());
            }

            // Time text
            String timeText = (s.dayTime() != null ? s.dayTime() : "Time TBD");

            String display = String.format(
                    "%s — %s — %s",
                    courseCode,
                    instructorName,
                    timeText
            );

            displayList.add(new SectionDisplay(s.sectionId(), display));
        }

        displayList.sort(SectionDisplay.CODE_COMPARATOR);
        sectionModel.addAll(displayList);
        sectionModel.setSelectedItem(previousSelection);
        log.debug("Loaded/Refreshed {} sections into assignment dropdown.", displayList.size());
    }

    private static class InstructorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Instructor instructor) {
                setText(instructor.name() + " (" + instructor.department() + ")");
            } else if (value == null) {
                if (index == -1) {
                    setText("-- Select Instructor --");
                } else {
                    setText("-- Unassigned / Optional --");
                }
            } else {
                setText("");
            }
            return this;
        }
    }

    private void createCourse() {
        String code = txtCourseCode.getText().trim();
        String title = txtCourseTitle.getText().trim();
        int credits = (Integer) spnCredits.getValue();

        if (code.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Course Code and Title cannot be empty.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ApiResponse<Void> response = adminApi.createCourse(code, title, credits);
        if (response.isSuccess()) {
            // Custom success message WITHOUT IDs
            JOptionPane.showMessageDialog(
                    this,
                    "Course \"" + code + " - " + title + "\" created successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
            txtCourseCode.setText("");
            txtCourseTitle.setText("");
            spnCredits.setValue(3);
            loadInitialData();
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createSection() {
        Course selectedCourse = (Course) cmbCourses.getSelectedItem();
        Instructor selectedInstructor = (Instructor) cmbInstructors.getSelectedItem();

        if (selectedCourse == null) {
            JOptionPane.showMessageDialog(this, "Please select a course.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int capacity = (Integer) spnCapacity.getValue();
        if (capacity <= 0) {
            JOptionPane.showMessageDialog(this, "Capacity must be a positive number.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int courseId = selectedCourse.courseId();
        Integer instructorId = (selectedInstructor != null) ? selectedInstructor.userId() : null;
        String dayTime = txtDayTime.getText().trim();
        String room = txtRoom.getText().trim();
        String semester = txtSemester.getText().trim();
        int year = (Integer) spnSectionYear.getValue();

        if (semester.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semester cannot be empty.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ApiResponse<Void> response =
                adminApi.createSection(courseId, instructorId, dayTime, room, capacity, semester, year);

        if (response.isSuccess()) {
            String instructorText = (selectedInstructor != null)
                    ? selectedInstructor.name()
                    : "no instructor assigned";
            JOptionPane.showMessageDialog(
                    this,
                    "Section created for " + selectedCourse.code() +
                            " (" + instructorText + ", " + semester + " " + year + ").",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
            txtDayTime.setText("");
            txtRoom.setText("");
            loadSectionsForAssignment(sectionModel.getSelectedItem());
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void assignInstructor(JComboBox<Instructor> instructorComboBox) {
        SectionDisplay selectedSectionDisp = (SectionDisplay) cmbSections.getSelectedItem();
        Instructor selectedInstructor = (Instructor) instructorComboBox.getSelectedItem();
        if (selectedSectionDisp == null) {
            JOptionPane.showMessageDialog(this, "Please select a section to assign.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int sectionId = selectedSectionDisp.sectionId();
        Integer instructorId = (selectedInstructor != null) ? selectedInstructor.userId() : null;
        ApiResponse<Void> response = adminApi.assignInstructor(sectionId, instructorId);

        if (response.isSuccess()) {
            String msg;
            if (selectedInstructor != null) {
                msg = "Assigned " + selectedInstructor.name() +
                        " to this section.";
            } else {
                msg = "Instructor" + selectedInstructor.name() + " unassigned from this section.";
            }
            JOptionPane.showMessageDialog(
                    this,
                    msg,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            loadSectionsForAssignment(selectedSectionDisp);
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSection() {
        SectionDisplay selectedSectionDisp = (SectionDisplay) cmbSections.getSelectedItem();
        if (selectedSectionDisp == null) {
            JOptionPane.showMessageDialog(this, "Please select a section to delete.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int sectionId = selectedSectionDisp.sectionId();
        int choice = JOptionPane.showConfirmDialog(this,
                "<html><font color='red'><b>WARNING:</b></font> Deleting section '" +
                        selectedSectionDisp.displayText() +
                        "' is permanent.<br>" +
                        "Any existing student enrollments and grades for this section may become inaccessible.<br><br>" +
                        "<b>Are you absolutely sure you want to delete this section?</b></html>",
                "Confirm Section Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            log.warn("UI: User confirmed deletion for section {}", sectionId);
            ApiResponse<Void> response = adminApi.deleteSection(sectionId);
            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Section deleted successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadSectionsForAssignment(null);
            } else {
                JOptionPane.showMessageDialog(this, response.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            log.info("UI: User cancelled section deletion for ID {}", sectionId);
        }
    }

    @Override
    public void refreshData() {
        loadInitialData();
    }
}
