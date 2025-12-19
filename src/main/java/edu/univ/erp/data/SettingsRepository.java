package edu.univ.erp.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class SettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(SettingsRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    // Helper method to get a specific setting value
    private Optional<String> getSettingValue(String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            log.error("SQL error getting setting '{}'", key, e);
        }
        return Optional.empty();
    }

    // Helper method to set a specific setting value
    private boolean setSettingValue(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = ?";
        log.debug("Attempting to set setting: key='{}', value='{}' with SQL: {}", key, value, sql);

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.setString(3, value);

            log.debug("Executing update for setting '{}'...", key);
            int rowsAffected = pstmt.executeUpdate();
            log.info("Finished executing update for setting '{}'. Rows affected: {}", key, rowsAffected);

            log.info("Set setting '{}' to '{}'", key, value);
            return true;

        } catch (SQLException e) {
            log.error("SQL error setting '{}' to '{}'", key, value, e);
            log.error("SQLState: {}, ErrorCode: {}", e.getSQLState(), e.getErrorCode()); // Log specifics
            return false;
        }
    }

    public boolean isMaintenanceModeOn() {
        return getSettingValue("maintenance_on").orElse("false").equalsIgnoreCase("true");
    }

    public boolean setMaintenanceMode(boolean enabled) {
        return setSettingValue("maintenance_on", enabled ? "true" : "false");
    }

    public String getCurrentSemester() {
        return getSettingValue("current_semester").orElse("Unknown");
    }

    public int getCurrentYear() {
        try {
            return Integer.parseInt(getSettingValue("current_year").orElse("0"));
        } catch (NumberFormatException e) {
            log.error("Invalid format for current_year setting in database.");
            return 0;
        }
    }

    public Optional<LocalDate> getDropDeadline() {
        Optional<String> dateString = getSettingValue("drop_deadline");
        if (dateString.isPresent()) {
            try {
                return Optional.of(LocalDate.parse(dateString.get()));
            } catch (DateTimeParseException e) {
                log.error("Invalid date format for drop_deadline setting: {}", dateString.get(), e);
            }
        }
        return Optional.empty();
    }

    public boolean setDropDeadline(LocalDate deadline) {
        if (deadline == null) {
            log.warn("Attempted to set null drop deadline.");
            return false;
        }
        return setSettingValue("drop_deadline", deadline.toString());
    }

    public Optional<LocalDate> getRegistrationDeadline() {
        Optional<String> dateString = getSettingValue("registration_deadline");
        if (dateString.isPresent()) {
            try {
                return Optional.of(LocalDate.parse(dateString.get()));
            } catch (DateTimeParseException e) {
                log.error("Invalid date format for registration_deadline setting: {}", dateString.get(), e);
            }
        }
        return Optional.empty();
    }

    public boolean setRegistrationDeadline(LocalDate deadline) {
        if (deadline == null) {
            log.warn("Attempted to set null registration deadline.");
            return false;
        }
        return setSettingValue("registration_deadline", deadline.toString());
    }
}
