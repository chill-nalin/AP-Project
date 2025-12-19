package edu.univ.erp.data;

import edu.univ.erp.domain.Instructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstructorProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(InstructorProfileRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    // Finds an instructor's profile by their user_id. This connects to the 'erp_db'.
    public Optional<Instructor> findProfileByUserId(int userId) {
        String sql = "SELECT name, department FROM instructors WHERE user_id = ?";

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Instructor instructor = new Instructor(
                            userId,
                            rs.getString("name"),
                            rs.getString("department")
                    );
                    return Optional.of(instructor);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error while finding instructor profile: {}", userId, e);
        }

        return Optional.empty();
    }

    public boolean createInstructor(int userId, String name, String department) {
        String sql = "INSERT INTO instructors (user_id, name, department) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, name);
            pstmt.setString(3, department);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Created instructor profile for user_id {}", userId);
                return true;
            } else {
                log.warn("Instructor profile creation failed for user_id {}", userId);
                return false;
            }
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                log.warn("Instructor profile creation failed for user_id {} due to constraint violation (duplicate user_id?).", userId);
            } else {
                log.error("SQL error creating instructor profile for user_id {}", userId, e);
            }
            return false;
        }
    }

    public List<Instructor> findAll() {
        List<Instructor> instructors = new ArrayList<>();
        String sql = "SELECT user_id, name, department FROM instructors ORDER BY name";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                instructors.add(new Instructor(
                        rs.getInt("user_id"), rs.getString("name"), rs.getString("department")
                ));
            }
        } catch (SQLException e) {
            log.error("SQL error finding all instructors", e);
        }
        return instructors;
    }
}