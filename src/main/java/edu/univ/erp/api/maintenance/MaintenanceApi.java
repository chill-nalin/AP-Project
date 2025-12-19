package edu.univ.erp.api.maintenance;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.data.SettingsRepository;
import edu.univ.erp.service.AdminException;
import edu.univ.erp.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

public class MaintenanceApi {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceApi.class);
    private final AdminService adminService = new AdminService();
    private final AccessControl accessControl = new AccessControl();
    private final SettingsRepository settingsRepo = new SettingsRepository();

    public ApiResponse<Void> setMaintenanceMode(boolean enabled) {
        try {
            adminService.setMaintenanceMode(enabled);
            return ApiResponse.success(null, "Maintenance mode set to: " + (enabled ? "ON" : "OFF"));
        } catch (AdminException e) {
            log.warn("API: Failed to set maintenance mode: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error setting maintenance mode", e);
            return ApiResponse.error("An unexpected error occurred.");
        }
    }

    public boolean isReadOnlyNow() {
        return accessControl.isMaintenanceModeOn();
    }

    public ApiResponse<LocalDate> getDropDeadline() {
        Optional<LocalDate> deadlineOpt = settingsRepo.getDropDeadline();
        if (deadlineOpt.isPresent()) {
            return ApiResponse.success(deadlineOpt.get(), "Drop deadline loaded.");
        } else {
            log.warn("API: Drop deadline not found or invalid in settings.");
            return ApiResponse.success(null, "Drop deadline is not set.");
        }
    }

    public ApiResponse<Void> setDropDeadline(LocalDate deadline) {
        try {
            adminService.setDropDeadline(deadline);
            return ApiResponse.success(null, "Drop deadline updated successfully to " + deadline + ".");
        } catch (AdminException e) {
            log.warn("API: Failed to set drop deadline: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error setting drop deadline", e);
            return ApiResponse.error("An unexpected error occurred.");
        }
    }

    public ApiResponse<LocalDate> getRegistrationDeadline() {
        Optional<LocalDate> deadlineOpt = settingsRepo.getRegistrationDeadline();
        if (deadlineOpt.isPresent()) {
            return ApiResponse.success(deadlineOpt.get(), "Registration deadline loaded.");
        } else {
            log.warn("API: Registration deadline not found or invalid in settings.");
            return ApiResponse.success(null, "Registration deadline is not set.");
        }
    }

    public ApiResponse<Void> setRegistrationDeadline(LocalDate deadline) {
        try {
            adminService.setRegistrationDeadline(deadline);
            return ApiResponse.success(null, "Registration deadline updated successfully to " + deadline + ".");
        } catch (AdminException e) {
            log.warn("API: Failed to set registration deadline: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error setting registration deadline", e);
            return ApiResponse.error("An unexpected error occurred.");
        }
    }
}