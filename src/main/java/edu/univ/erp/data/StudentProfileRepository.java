package edu.univ.erp.data;

import edu.univ.erp.domain.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class StudentProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    public Optional<Student> findProfileByUserId(int userId) {
        String sql = "SELECT roll_no, program FROM students WHERE user_id = ?";

        // Uses the ERP-specific connection pool
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student(
                            userId,
                            rs.getString("roll_no"),
                            rs.getString("program")
                    );
                    return Optional.of(student);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error while finding student profile: {}", userId, e);
        }

        return Optional.empty();
    }

    public Optional<Student> findById(int userId) {
        return findProfileByUserId(userId);
    }

    public boolean createStudent(int userId, String rollNo, String program, int year) {
        String sql = "INSERT INTO students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, rollNo);
            pstmt.setString(3, program);
            pstmt.setInt(4, year);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Created student profile for user_id {}", userId);
                return true;
            } else {
                log.warn("Student profile creation failed for user_id {}", userId);
                return false;
            }
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                log.warn("Student profile creation failed for user_id {} due to constraint violation (duplicate user_id or roll_no?).", userId);
            } else {
                log.error("SQL error creating student profile for user_id {}", userId, e);
            }
            return false;
        }
    }
}