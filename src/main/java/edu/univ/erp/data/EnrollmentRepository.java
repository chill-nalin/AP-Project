package edu.univ.erp.data;

import edu.univ.erp.domain.Enrollment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Repository for the 'enrollments' table in the 'erp_db'.
public class EnrollmentRepository {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    // Checks if an enrollment already exists for a given student and section.
    public boolean exists(int studentId, int sectionId) {
        String sql = "SELECT 1 FROM enrollments WHERE student_id = ? AND section_id = ? LIMIT 1";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, sectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("SQL error checking enrollment existence for student {} in section {}", studentId, sectionId, e);
            return false;
        }
    }

    // Creates a new enrollment record.
    public boolean create(int studentId, int sectionId) {
        String sql = "INSERT INTO enrollments (student_id, section_id, status) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, sectionId);
            pstmt.setString(3, "Enrolled");
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Created new enrollment for student {} in section {}", studentId, sectionId);
                return true;
            } else {
                log.warn("Enrollment creation failed unexpectedly for student {} in section {}", studentId, sectionId);
                return false;
            }
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                log.warn("Attempted to create duplicate enrollment for student {} in section {}", studentId, sectionId);
            } else {
                log.error("SQL error creating enrollment for student {} in section {}", studentId, sectionId, e);
            }
            return false;
        }
    }

    // Finds all enrollments for a specific student.
    public List<Enrollment> findByStudentId(int studentId) {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT enrollment_id, student_id, section_id, status FROM enrollments WHERE student_id = ?";

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Enrollment enrollment = new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getInt("student_id"),
                            rs.getInt("section_id"),
                            rs.getString("status")
                    );
                    enrollments.add(enrollment);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error while finding enrollments for student {}", studentId, e);
        }
        return enrollments;
    }

    // Deletes an enrollment record by its primary key.
    public boolean deleteById(int enrollmentId) {
        String sql = "DELETE FROM enrollments WHERE enrollment_id = ?";

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, enrollmentId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                log.info("Deleted enrollment with ID: {}", enrollmentId);
                return true;
            } else {
                log.warn("No enrollment found with ID {} to delete.", enrollmentId);
                return false;
            }
        } catch (SQLException e) {
            log.error("SQL error while deleting enrollment {}", enrollmentId, e);
            return false;
        }
    }

    public List<Enrollment> findBySectionId(int sectionId) {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT enrollment_id, student_id, section_id, status FROM enrollments WHERE section_id = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Enrollment enrollment = new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getInt("student_id"),
                            rs.getInt("section_id"),
                            rs.getString("status")
                    );
                    enrollments.add(enrollment);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error finding enrollments for section {}", sectionId, e);
        }
        return enrollments;
    }

    public Optional<Enrollment> findById(int enrollmentId) {
        String sql = "SELECT enrollment_id, student_id, section_id, status FROM enrollments WHERE enrollment_id = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enrollmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Enrollment enrollment = new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getInt("student_id"),
                            rs.getInt("section_id"),
                            rs.getString("status")
                    );
                    return Optional.of(enrollment);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error finding enrollment by id: {}", enrollmentId, e);
        }
        return Optional.empty();
    }

    public int countEnrollmentsBySection(int sectionId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE section_id = ?";
        try (Connection con = dbManager.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count enrollments", e);
        }
        return 0;
    }
}