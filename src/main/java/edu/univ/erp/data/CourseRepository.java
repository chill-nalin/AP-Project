package edu.univ.erp.data;

import edu.univ.erp.domain.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseRepository {

    private static final Logger log = LoggerFactory.getLogger(CourseRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    public Optional<Course> findById(int courseId) {
        String sql = "SELECT course_id, code, title, credits FROM courses WHERE course_id = ?";

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Course course = new Course(
                            rs.getInt("course_id"),
                            rs.getString("code"),
                            rs.getString("title"),
                            rs.getInt("credits")
                    );
                    return Optional.of(course);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error while finding course by id: {}", courseId, e);
        }

        return Optional.empty();
    }

    public boolean createCourse(String code, String title, int credits) {
        String sql = "INSERT INTO courses (code, title, credits) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, title);
            pstmt.setInt(3, credits);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Created course: {} - {}", code, title);
                return true;
            } else {
                log.warn("Course creation failed for code {}", code);
                return false;
            }
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) { // Handle UNIQUE code violation
                log.warn("Course creation failed for code {}: Code already exists.", code);
            } else {
                log.error("SQL error creating course {}", code, e);
            }
            return false;
        }
    }

    public List<Course> findAll() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, code, title, credits FROM courses ORDER BY code";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                courses.add(new Course(
                        rs.getInt("course_id"), rs.getString("code"),
                        rs.getString("title"), rs.getInt("credits")
                ));
            }
        } catch (SQLException e) {
            log.error("SQL error finding all courses", e);
        }
        return courses;
    }

    public boolean updateCourse(int courseId, String newTitle, int newCredits) {
        String sql = "UPDATE courses SET title = ?, credits = ? WHERE course_id = ?";
        try (Connection con = dbManager.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newTitle);
            ps.setInt(2, newCredits);
            ps.setInt(3, courseId);

            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update course with ID " + courseId, e);
        }
    }
}