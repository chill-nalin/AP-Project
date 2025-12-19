package edu.univ.erp.data;

import edu.univ.erp.domain.Grade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GradeRepository {

    private static final Logger log = LoggerFactory.getLogger(GradeRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    public List<Grade> findByEnrollmentId(int enrollmentId) {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT grade_id, enrollment_id, component, score, final_grade " +
                "FROM grades WHERE enrollment_id = ?";

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, enrollmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // *** FIX: Retrieve as BigDecimal, then convert to Double ***
                    BigDecimal scoreBigDecimal = rs.getBigDecimal("score");
                    // Convert to Double, keeping null if the BigDecimal was null
                    Double scoreDouble = (scoreBigDecimal != null) ? scoreBigDecimal.doubleValue() : null;
                    // *** END FIX ***

                    Grade grade = new Grade(
                            rs.getInt("grade_id"),
                            rs.getInt("enrollment_id"),
                            rs.getString("component"),
                            scoreDouble, // Use the correctly converted Double (or null)
                            rs.getString("final_grade")
                    );
                    grades.add(grade);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error while finding grades for enrollment {}", enrollmentId, e);
        }
        return grades;
    }

    // *** NEW METHOD ***
    public Optional<Grade> findByEnrollmentAndComponent(int enrollmentId, String component) {
        String sql = "SELECT grade_id, enrollment_id, component, score, final_grade " +
                "FROM grades WHERE enrollment_id = ? AND component = ?";
        log.debug("Finding existing grade for enrollmentId={}, component='{}'", enrollmentId, component); // Log entry

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enrollmentId);
            pstmt.setString(2, component);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal scoreBigDecimal = rs.getBigDecimal("score");
                    Double scoreDouble = (scoreBigDecimal != null) ? scoreBigDecimal.doubleValue() : null;
                    Grade grade = new Grade(
                            rs.getInt("grade_id"), rs.getInt("enrollment_id"),
                            rs.getString("component"), scoreDouble, rs.getString("final_grade")
                    );
                    log.debug("Found existing grade with grade_id: {}", grade.gradeId()); // Log success + ID
                    return Optional.of(grade);
                } else {
                    log.debug("No existing grade found for enrollmentId={}, component='{}'. Will perform INSERT.", enrollmentId, component); // Log not found
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            log.error("SQL error finding grade for enrollment {}, component {}", enrollmentId, component, e);
        }
        return Optional.empty();
    }

    // *** NEW METHOD ***
    public boolean saveOrUpdateScore(int enrollmentId, String component, Double score) {
        log.debug("Attempting to save/update score: enrollmentId={}, component='{}', score={}", enrollmentId, component, score);
        // 1. Check if a grade for this component already exists
        Optional<Grade> existingGradeOpt = findByEnrollmentAndComponent(enrollmentId, component);

        String sql;
        boolean isUpdate = existingGradeOpt.isPresent(); // Determine if it's an update

        if (isUpdate) {
            sql = "UPDATE grades SET score = ? WHERE grade_id = ?";
            log.debug("Preparing UPDATE statement for grade_id {}", existingGradeOpt.get().gradeId());
        } else {
            sql = "INSERT INTO grades (enrollment_id, component, score) VALUES (?, ?, ?)";
            log.debug("Preparing INSERT statement for enrollmentId {}, component '{}'", enrollmentId, component);
        }

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Bind parameters based on whether it's an UPDATE or INSERT
            if (isUpdate) {
                // UPDATE: score = ?, grade_id = ?
                if (score == null) {
                    pstmt.setNull(1, Types.DECIMAL); // Parameter 1: score
                    log.trace("Binding NULL to parameter 1 (score)");
                } else {
                    pstmt.setDouble(1, score);       // Parameter 1: score
                    log.trace("Binding {} to parameter 1 (score)", score);
                }
                pstmt.setInt(2, existingGradeOpt.get().gradeId()); // Parameter 2: grade_id
                log.trace("Binding {} to parameter 2 (grade_id)", existingGradeOpt.get().gradeId());
            } else {
                // INSERT: enrollment_id = ?, component = ?, score = ?
                pstmt.setInt(1, enrollmentId);        // Parameter 1: enrollment_id
                pstmt.setString(2, component);        // Parameter 2: component
                if (score == null) {
                    pstmt.setNull(3, Types.DECIMAL); // Parameter 3: score
                    log.trace("Binding NULL to parameter 3 (score)");
                } else {
                    pstmt.setDouble(3, score);       // Parameter 3: score
                    log.trace("Binding {} to parameter 3 (score)", score);
                }
                log.trace("Binding {} to parameter 1 (enrollment_id)", enrollmentId);
                log.trace("Binding '{}' to parameter 2 (component)", component);
            }

            log.debug("Executing SQL: {}", sql.replaceFirst("\\?", score != null ? score.toString() : "NULL")); // Basic logging
            int rowsAffected = pstmt.executeUpdate();
            log.info("Finished executing save/update. Rows affected: {}", rowsAffected);

            // For INSERT, 1 row is success. For UPDATE, 1 row is success.
            // 0 rows on UPDATE means the grade_id didn't exist (unlikely if find succeeded), or value was same.
            // Let's consider 0 rows affected on UPDATE as potentially okay if value didn't change, but log warning.
            if (rowsAffected > 0) {
                return true;
            } else if (isUpdate) {
                log.warn("UPDATE operation affected 0 rows for grade_id {}. Value might not have changed.", existingGradeOpt.get().gradeId());
                return true; // Still consider it success as state is achieved
            } else {
                log.error("INSERT operation affected 0 rows unexpectedly.");
                return false; // Insert failing is definitely an error
            }

        } catch (SQLException e) {
            log.error("SQL error during save/update score for enrollment {}, component '{}': {}", enrollmentId, component, e.getMessage());
            log.error("SQLState: {}, ErrorCode: {}", e.getSQLState(), e.getErrorCode());
            return false;
        }
    }

    public boolean updateFinalGradeForEnrollment(int enrollmentId, String finalLetterGrade) {
        // Update the final grade on all existing rows for this enrollment.
        // This ensures the final grade is visible regardless of which component is viewed,
        // though conceptually it applies to the enrollment as a whole.
        String sql = "UPDATE grades SET final_grade = ? WHERE enrollment_id = ?";
        log.debug("Updating final grade for enrollment {} to {}", enrollmentId, finalLetterGrade);

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (finalLetterGrade == null || finalLetterGrade.isBlank()) {
                pstmt.setNull(1, Types.VARCHAR);
            } else {
                pstmt.setString(1, finalLetterGrade);
            }
            pstmt.setInt(2, enrollmentId);

            int rowsAffected = pstmt.executeUpdate();
            // It's okay if 0 rows are affected if no grades were entered yet,
            // but we log a warning. The service layer should ideally handle this.
            if (rowsAffected == 0) {
                log.warn("Attempted to update final grade for enrollment {}, but no grade rows found/updated.", enrollmentId);
            }
            // Consider success even if 0 rows affected, as the state is technically achieved.
            return true;
        } catch (SQLException e) {
            log.error("SQL error updating final grade for enrollment {}", enrollmentId, e);
            return false;
        }
    }

    public boolean deleteByEnrollmentId(int enrollmentId) {
        String sql = "DELETE FROM grades WHERE enrollment_id = ?";
        log.debug("Attempting to delete all grades for enrollment_id {}", enrollmentId);

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, enrollmentId);
            int rowsAffected = pstmt.executeUpdate();
            // It's okay if 0 rows are affected (no grades to delete)
            log.info("Deleted {} grade records for enrollment_id {}", rowsAffected, enrollmentId);
            return true; // Return true as long as no exception occurred

        } catch (SQLException e) {
            log.error("SQL error deleting grades for enrollment_id {}", enrollmentId, e);
            return false;
        }
    }
}