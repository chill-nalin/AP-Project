package edu.univ.erp.api.auth;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AuthException;
import edu.univ.erp.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthApi {

    private static final Logger log = LoggerFactory.getLogger(AuthApi.class);
    private final AuthService authService;
    private final SessionManager sessionManager = SessionManager.getInstance();

    public AuthApi() {
        this.authService = new AuthService();
    }

    public ApiResponse<User> login(String username, String password) {
        try {
            // 1. Call the service "brain"
            User user = authService.login(username, password);

            // 2. On success, wrap in a success response
            log.info("API: Login successful for user '{}'", username);
            return ApiResponse.success(user, "Login successful!");

        } catch (AuthException e) {
            // 3. On failure, log the exception and return an error response
            log.warn("API: Login failed for user '{}': {}", username, e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<Void> changePassword(String oldPassword, String newPassword) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error("You must be logged in to change your password.");
        }

        try {
            // Pass user ID and username for verification in service
            authService.changePassword(currentUser.userId(), currentUser.username(), oldPassword, newPassword);
            log.info("API: Password change successful for user {}", currentUser.username());
            return ApiResponse.success(null, "Password changed successfully!");
        } catch (AuthException e) {
            log.warn("API: Password change failed for user {}: {}", currentUser.username(), e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error changing password for user {}", currentUser.username(), e);
            return ApiResponse.error("An unexpected error occurred. Please try again.");
        }
    }
}