package edu.univ.erp.service;

import edu.univ.erp.auth.PasswordHasher;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.auth.UserAuthData;
import edu.univ.erp.auth.UserAuthRepository;
import edu.univ.erp.data.InstructorProfileRepository;
import edu.univ.erp.data.StudentProfileRepository;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

// It coordinates repositories and services to log a user in.
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAuthRepository authRepository = new UserAuthRepository();
    private final StudentProfileRepository studentRepository = new StudentProfileRepository();
    private final InstructorProfileRepository instructorRepository = new InstructorProfileRepository();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // Attempts to log in a user.
    public User login(String username, String password) throws AuthException {
        log.info("Login attempt for user: {}", username);

        // 1. Find user auth data
        UserAuthData authData = authRepository.findUserAuthDataByUsername(username)
                .orElseThrow(() -> new AuthException("Invalid username or password."));

        // 2. Check the password
        boolean passwordMatches = PasswordHasher.checkPassword(password, authData.passwordHash());

        if (!passwordMatches) {
            log.warn("Invalid password for user: {}", username);
            throw new AuthException("Invalid username or password.");
        }

        // 3. Update last login on success
        authRepository.updateLastLogin(authData.userId());

        // 4. Load the user's profile from the ERP database
        Object profile = loadUserProfile(authData.userId(), authData.role());

        // 5. Create the complete User session object
        User user = new User(authData.userId(), username, authData.role(), profile);

        // 6. Start the session
        sessionManager.startSession(user);
        log.info("Login successful for user: {}", username);
        return user;
    }

    // Changes the password for a logged-in user after verifying the old password.
    public void changePassword(int userId, String username, String oldPassword, String newPassword) throws AuthException {
        log.info("Password change attempt for user_id {}", userId);

        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new AuthException("New password must be at least 6 characters long.");
        }
        if (newPassword.equals(oldPassword)) {
            throw new AuthException("New password cannot be the same as the old password.");
        }

        // Fetch current user auth data using username for hash comparison
        Optional<UserAuthData> authDataOpt = authRepository.findUserAuthDataByUsername(username);

        // Security check: ensure the username corresponds to the userId from session
        if(authDataOpt.isEmpty() || authDataOpt.get().userId() != userId) {
            log.error("Security mismatch during password change for userId {} / username {}", userId, username);
            throw new AuthException("Authentication error during password change.");
        }
        UserAuthData authData = authDataOpt.get();

        // Verify old password
        if (!PasswordHasher.checkPassword(oldPassword, authData.passwordHash())) {
            log.warn("Incorrect old password provided for password change user_id {}", userId);
            throw new AuthException("Incorrect current password.");
        }

        // Hash the new password
        String newHashedPassword = PasswordHasher.hash(newPassword);

        // Update the hash in the database
        boolean success = authRepository.updatePasswordHash(userId, newHashedPassword);

        if (!success) {
            log.error("Failed to update password hash in database for user_id {}", userId);
            throw new AuthException("Failed to update password. Please try again.");
        }

        log.info("Password successfully changed for user_id {}", userId);
    }

    // Helper method to fetch the correct profile (Student, Instructor) from the erp_db.
    private Object loadUserProfile(int userId, Role role) throws AuthException {
        switch (role) {
            case Student:
                return studentRepository.findProfileByUserId(userId)
                        .orElseThrow(() -> {
                            log.error("Data integrity error: User {} (Student) has auth record but no profile in erp_db.", userId);
                            return new AuthException("User profile data is missing. Please contact administrator.");
                        });
            case Instructor:
                return instructorRepository.findProfileByUserId(userId)
                        .orElseThrow(() -> {
                            log.error("Data integrity error: User {} (Instructor) has auth record but no profile in erp_db.", userId);
                            return new AuthException("User profile data is missing. Please contact administrator.");
                        });
            case Admin:
                log.debug("Admin user {} logged in. No profile expected.", userId);
                return null;
            default:
                log.error("Unknown role {} encountered for user {}", role, userId);
                throw new AuthException("An internal error occurred (unknown user role).");
        }
    }
}