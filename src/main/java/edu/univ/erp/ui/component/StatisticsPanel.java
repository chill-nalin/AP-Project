package edu.univ.erp.ui.component;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.instructor.InstructorApi;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

// A JPanel that displays section statistics using JFreeChart.
public class StatisticsPanel extends JPanel implements Refreshable {

    private static final Logger log = LoggerFactory.getLogger(StatisticsPanel.class);
    private final InstructorApi instructorApi = new InstructorApi();
    private final int sectionId;

    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_TEXT_PRIMARY = new Color(233, 236, 239);
    private static final Color COLOR_TEXT_SECONDARY = new Color(173, 181, 189);

    private ChartPanel chartPanel;
    private JPanel contentPanel;

    public StatisticsPanel(int sectionId) {
        this.sectionId = sectionId;

        setLayout(new MigLayout(
                "wrap 1, fill, insets 10",
                "[grow,fill]",
                "[]10[grow]"
        ));
        setBackground(COLOR_BACKGROUND);
        setBorder(null);

        // Title
        JLabel titleLabel = new JLabel("Class Averages");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(COLOR_TEXT_PRIMARY);
        add(titleLabel, "growx, wrap");

        contentPanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[grow]"));
        contentPanel.setOpaque(false);

        chartPanel = new ChartPanel(null);
        chartPanel.setPreferredSize(new Dimension(300, 200));
        chartPanel.setOpaque(false);

        contentPanel.add(chartPanel, "grow");
        add(contentPanel, "grow");

        loadStatistics();
    }

    // Data loading
    private void loadStatistics() {
        log.info("Loading statistics for section {}", sectionId);

        ApiResponse<Map<String, Double>> response = instructorApi.getSectionStatistics(sectionId);

        if (response.isSuccess()) {
            Map<String, Double> stats = response.getData();
            if (stats == null || stats.isEmpty()) {
                log.info("No statistics data available for section {}", sectionId);
                displayNoDataMessage();
            } else {
                displayStats(stats);
            }
        } else {
            log.error("Failed to load statistics: {}", response.getMessage());
            displayErrorMessage(response.getMessage());
        }
    }

    private void displayStats(Map<String, Double> stats) {
        contentPanel.removeAll();

        // Build multi-line text
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<div style='color: #E9ECEF; font-size: 13px;'>");

        sb.append("EndSem: ").append(stats.getOrDefault("EndSem", 0.0)).append("<br>");
        sb.append("Midterm: ").append(stats.getOrDefault("Midterm", 0.0)).append("<br>");
        sb.append("Quiz: ").append(stats.getOrDefault("Quiz", 0.0)).append("<br>");

        sb.append("</div></html>");

        JLabel statsLabel = new JLabel(sb.toString());
        statsLabel.setVerticalAlignment(SwingConstants.TOP);

        contentPanel.add(statsLabel, "align left, wrap");
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // Message helpers
    private void displayNoDataMessage() {
        contentPanel.removeAll();
        JLabel msg = new JLabel("No grade data available to calculate statistics.");
        msg.setForeground(COLOR_TEXT_SECONDARY);
        msg.setFont(new Font("SansSerif", Font.PLAIN, 13));
        contentPanel.add(msg, "align center");
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void displayErrorMessage(String message) {
        contentPanel.removeAll();
        JLabel errorLabel = new JLabel("Error loading stats: " + message);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        contentPanel.add(errorLabel, "align center");
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    @Override
    public void refreshData() {
        loadStatistics();
    }
}