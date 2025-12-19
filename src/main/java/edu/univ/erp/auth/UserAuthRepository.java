package edu.univ.erp.auth;

import edu.univ.erp.data.DbManager;
import edu.univ.erp.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.Optional;

public class UserAuthRepository {

    private static final Logger log = LoggerFactory.getLogger(UserAuthRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    public Optional<UserAuthData> findUserAuthDataByUsername(String username) {
        String sql = "SELECT user_id, role, password_hash FROM users_auth WHERE username = ?";
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserAuthData data = new UserAuthData(
                            rs.getInt("user_id"),
                            Role.valueOf(rs.getString("role")),
                            rs.getString("password_hash")
                    );
                    return Optional.of(data);
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            log.error("Error finding user auth data by username: {}", username, e);
        }
        return Optional.empty();
    }

    public Optional<Integer> createUserAuth(String username, String hashedPassword, Role role) {
        String sql = "INSERT INTO users_auth (username, password_hash, role, status) VALUES (?, ?, ?, 'Active')";
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, role.name());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newUserId = generatedKeys.getInt(1);
                        log.info("Created user auth record for username '{}' with user_id {}", username, newUserId);
                        return Optional.of(newUserId);
                    }
                }
            }
            log.warn("User auth creation failed for username '{}'", username);
            return Optional.empty();
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                log.warn("Username '{}' already exists.", username);
            } else {
                log.error("SQL error creating user auth for username '{}'", username, e);
            }
            return Optional.empty();
        }
    }

    public boolean updatePasswordHash(int userId, String newHashedPassword) {
        String sql = "UPDATE users_auth SET password_hash = ? WHERE user_id = ?";
        log.debug("Attempting to update password hash for user_id {}", userId);
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newHashedPassword);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Successfully updated password hash for user_id {}", userId);
                return true;
            } else {
                log.warn("Password hash update failed for user_id {}: User not found?", userId);
                return false;
            }
        } catch (SQLException e) {
            log.error("SQL error updating password hash for user_id {}", userId, e);
            return false;
        }
    }

    public boolean deleteUserAuthById(int userId) {
        String sql = "DELETE FROM users_auth WHERE user_id = ?";
        log.warn("Attempting to roll back auth record creation for user_id {}", userId);
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Successfully rolled back (deleted) auth record for user_id {}", userId);
                return true;
            } else {
                log.error("Rollback of auth record failed for user_id {}: Record not found?", userId);
                return false;
            }
        } catch (SQLException e) {
            log.error("SQL error during auth record rollback for user_id {}", userId, e);
            return false;
        }
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE users_auth SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL error updating last_login for user_id {}", userId, e);
        }
    }
}