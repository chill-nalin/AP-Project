package edu.univ.erp.ui.component;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.student.StudentApi;
import edu.univ.erp.api.types.TimetableEntry;
import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TimetablePanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(TimetablePanel.class);

    private final StudentApi studentApi = new StudentApi();
    private final User currentUser;

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_CARD_BG = new Color(30, 30, 30);
    private static final Color COLOR_DAY_PANEL_BG = new Color(34, 34, 34);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);
    private static final Color COLOR_BORDER = new Color(60, 60, 60);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_DAY_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_LABEL_BOLD = new Font("SansSerif", Font.BOLD, 13);

    private JPanel contentPanel;

    public TimetablePanel(User user) {
        this.currentUser = user;

        setLayout(new MigLayout(
                "wrap 1, fillx, insets 20",
                "[grow,fill]",
                "[]15[grow]"
        ));
        setBackground(COLOR_BACKGROUND);

        // Header
        JPanel headerPanel = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("My Timetable");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(COLOR_TEXT_PRIMARY);
        headerPanel.add(titleLabel, "growx");

        add(headerPanel, "growx, wrap");
        contentPanel = new JPanel(new MigLayout(
                "wrap 1, fillx, gapy 10",
                "[grow,fill]",
                ""
        ));
        contentPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, "grow");

        loadTimetableData();
    }

    private void loadTimetableData() {
        log.info("Loading timetable for student {}", currentUser.userId());

        contentPanel.removeAll();

        ApiResponse<List<TimetableEntry>> response =
                studentApi.getMyTimetable(currentUser.userId());

        if (!response.isSuccess()) {
            log.error("Failed to load timetable: {}", response.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load your timetable: " + response.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            showEmptyState("Unable to load your timetable.");
            refreshLayout();
            return;
        }

        List<TimetableEntry> timetable = response.getData();
        if (timetable == null || timetable.isEmpty()) {
            showEmptyState("No classes scheduled in your timetable.");
            refreshLayout();
            return;
        }

        Map<String, List<TimetableEntry>> byDay = groupByDay(timetable);

        String[] dayOrder = {
                "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday",
                "Sunday", "Unknown"
        };

        for (String day : dayOrder) {
            List<TimetableEntry> dayEntries = byDay.get(day);
            if (dayEntries == null || dayEntries.isEmpty()) {
                continue;
            }
            addDaySection(day, dayEntries);
        }

        refreshLayout();
        log.info("Timetable data loaded into calendar layout.");
    }

    private void showEmptyState(String message) {
        JPanel emptyPanel = new JPanel(new MigLayout("wrap 1, insets 10 10 10 10", "[grow,fill]", "[]"));
        emptyPanel.setOpaque(true);
        emptyPanel.setBackground(COLOR_CARD_BG);
        emptyPanel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JLabel lbl = new JLabel(message, SwingConstants.CENTER);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT_SECONDARY);
        lbl.setBorder(new EmptyBorder(10, 10, 10, 10));

        emptyPanel.add(lbl, "growx");
        contentPanel.add(emptyPanel, "growx");
    }

    private void refreshLayout() {
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private Map<String, List<TimetableEntry>> groupByDay(List<TimetableEntry> entries) {
        Map<String, List<TimetableEntry>> map = new LinkedHashMap<>();

        for (TimetableEntry entry : entries) {
            String dayTime = entry.dayTime();
            String key = normalizeDay(dayTime);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }
        return map;
    }

    private String normalizeDay(String dayTime) {
        if (dayTime == null || dayTime.isBlank()) {
            return "Unknown";
        }

        String lower = dayTime.toLowerCase();

        // Split by common delimiters: space, comma, slash, dash
        String[] tokens = lower.split("[,\\s/]+");
        for (String t : tokens) {
            if (t.startsWith("mon")) return "Monday";
            if (t.startsWith("tue")) return "Tuesday";
            if (t.startsWith("wed")) return "Wednesday";
            if (t.startsWith("thu")) return "Thursday";
            if (t.startsWith("fri")) return "Friday";
            if (t.startsWith("sat")) return "Saturday";
            if (t.startsWith("sun")) return "Sunday";
        }

        return "Unknown";
    }

    private void addDaySection(String dayName, List<TimetableEntry> entries) {
        JPanel dayPanel = new JPanel(new MigLayout(
                "wrap 1, fillx, insets 10 10 10 10",
                "[grow,fill]",
                "[]10[]"
        ));
        dayPanel.setOpaque(true);
        dayPanel.setBackground(COLOR_DAY_PANEL_BG);
        dayPanel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JLabel dayLabel = new JLabel(
                "Unknown".equals(dayName) ? "Unknown / TBA" : dayName
        );
        dayLabel.setFont(FONT_DAY_TITLE);
        dayLabel.setForeground(COLOR_TEXT_PRIMARY);
        dayPanel.add(dayLabel, "growx");

        for (TimetableEntry entry : entries) {
            dayPanel.add(createEntryCard(entry), "growx");
        }

        contentPanel.add(dayPanel, "growx");
    }

    private JComponent createEntryCard(TimetableEntry entry) {
        JPanel card = new JPanel(new MigLayout(
                "wrap 1, fillx, insets 6 10 6 10",
                "[grow,fill]",
                "[]2[]2[]"
        ));
        card.setOpaque(true);
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        String code = safe(entry.courseCode());
        String title = safe(entry.courseTitle());
        String dayTime = safe(entry.dayTime());
        String room = safe(entry.room());
        String instructor = safe(entry.instructorName());

        JLabel lblCourse = new JLabel(code + " — " + title);
        lblCourse.setFont(FONT_LABEL_BOLD);
        lblCourse.setForeground(COLOR_TEXT_PRIMARY);
        card.add(lblCourse, "growx");

        JLabel lblTimeRoom = new JLabel(dayTime + (room.isBlank() ? "" : "  •  " + room));
        lblTimeRoom.setFont(FONT_LABEL);
        lblTimeRoom.setForeground(COLOR_TEXT_SECONDARY);
        card.add(lblTimeRoom, "growx");

        JLabel lblInstructor = new JLabel(
                instructor.isBlank() ? "Instructor: TBA" : "Instructor: " + instructor
        );
        lblInstructor.setFont(FONT_LABEL);
        lblInstructor.setForeground(COLOR_TEXT_SECONDARY);
        card.add(lblInstructor, "growx");

        return card;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    @Override
    public void refreshData() {
        loadTimetableData();
    }
}
