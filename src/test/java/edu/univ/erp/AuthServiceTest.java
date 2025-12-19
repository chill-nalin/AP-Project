package edu.univ.erp;

import edu.univ.erp.auth.PasswordHasher;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.auth.UserAuthData;
import edu.univ.erp.auth.UserAuthRepository;
import edu.univ.erp.data.InstructorProfileRepository;
import edu.univ.erp.data.StudentProfileRepository;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AuthException;
import edu.univ.erp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    @Mock private UserAuthRepository authRepo;
    @Mock private StudentProfileRepository studentRepo;
    @Mock private InstructorProfileRepository instructorRepo;
    @Mock private SessionManager sessionManager;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService();
        injectMock(authService, "authRepository", authRepo);
        injectMock(authService, "studentRepository", studentRepo);
        injectMock(authService, "instructorRepository", instructorRepo);
        injectMock(authService, "sessionManager", sessionManager);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    @Test
    @DisplayName("Login: Success - Student Role")
    void login_Success_Student() throws AuthException {
        String username = "stu1";
        String password = "pass123";
        String hashedPassword = PasswordHasher.hash(password);

        UserAuthData authData = new UserAuthData(100, Role.Student, hashedPassword);
        Student studentProfile = new Student(100, "2024001", "B.Tech CSE");

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));
        when(studentRepo.findProfileByUserId(100)).thenReturn(Optional.of(studentProfile));

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(password, hashedPassword)).thenReturn(true);

            User result = authService.login(username, password);

            assertNotNull(result);
            assertEquals(100, result.userId());
            assertEquals(username, result.username());
            assertEquals(Role.Student, result.role());
            assertEquals(studentProfile, result.profile());

            verify(authRepo).updateLastLogin(100);
            verify(sessionManager).startSession(any(User.class));
        }
    }

    @Test
    @DisplayName("Login: Success - Instructor Role")
    void login_Success_Instructor() throws AuthException {
        String username = "inst1";
        String password = "pass123";
        String hashedPassword = PasswordHasher.hash(password);

        UserAuthData authData = new UserAuthData(200, Role.Instructor, hashedPassword);
        Instructor instructorProfile = new Instructor(200, "Dr. Smith", "Computer Science");

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));
        when(instructorRepo.findProfileByUserId(200)).thenReturn(Optional.of(instructorProfile));

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(password, hashedPassword)).thenReturn(true);

            User result = authService.login(username, password);

            assertNotNull(result);
            assertEquals(200, result.userId());
            assertEquals(username, result.username());
            assertEquals(Role.Instructor, result.role());
            assertEquals(instructorProfile, result.profile());

            verify(authRepo).updateLastLogin(200);
            verify(sessionManager).startSession(any(User.class));
        }
    }

    @Test
    @DisplayName("Login: Success - Admin Role (No Profile)")
    void login_Success_Admin() throws AuthException {
        String username = "admin1";
        String password = "pass123";
        String hashedPassword = PasswordHasher.hash(password);

        UserAuthData authData = new UserAuthData(1, Role.Admin, hashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(password, hashedPassword)).thenReturn(true);

            User result = authService.login(username, password);

            assertNotNull(result);
            assertEquals(1, result.userId());
            assertEquals(username, result.username());
            assertEquals(Role.Admin, result.role());
            assertNull(result.profile());

            verify(authRepo).updateLastLogin(1);
            verify(sessionManager).startSession(any(User.class));
        }
    }

    @Test
    @DisplayName("Login: Fail - Invalid Username")
    void login_Fail_InvalidUsername() {
        String username = "nonexistent";
        String password = "pass123";

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(username, password);
        });

        assertEquals("Invalid username or password.", exception.getMessage());
        verify(authRepo, never()).updateLastLogin(anyInt());
        verify(sessionManager, never()).startSession(any(User.class));
    }

    @Test
    @DisplayName("Login: Fail - Invalid Password")
    void login_Fail_InvalidPassword() {
        String username = "stu1";
        String password = "wrongpass";
        String hashedPassword = PasswordHasher.hash("correctpass");

        UserAuthData authData = new UserAuthData(100, Role.Student, hashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(password, hashedPassword)).thenReturn(false);

            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.login(username, password);
            });

            assertEquals("Invalid username or password.", exception.getMessage());
            verify(authRepo, never()).updateLastLogin(anyInt());
            verify(sessionManager, never()).startSession(any(User.class));
        }
    }

    @Test
    @DisplayName("Login: Fail - Student Profile Missing")
    void login_Fail_StudentProfileMissing() {
        String username = "stu1";
        String password = "pass123";
        String hashedPassword = PasswordHasher.hash(password);

        UserAuthData authData = new UserAuthData(100, Role.Student, hashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));
        when(studentRepo.findProfileByUserId(100)).thenReturn(Optional.empty());

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(password, hashedPassword)).thenReturn(true);

            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.login(username, password);
            });

            assertEquals("User profile data is missing. Please contact administrator.", exception.getMessage());
            verify(authRepo).updateLastLogin(100);
            verify(sessionManager, never()).startSession(any(User.class));
        }
    }

    @Test
    @DisplayName("Login: Fail - Instructor Profile Missing")
    void login_Fail_InstructorProfileMissing() {
        String username = "inst1";
        String password = "pass123";
        String hashedPassword = PasswordHasher.hash(password);

        UserAuthData authData = new UserAuthData(200, Role.Instructor, hashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));
        when(instructorRepo.findProfileByUserId(200)).thenReturn(Optional.empty());

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(password, hashedPassword)).thenReturn(true);

            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.login(username, password);
            });

            assertEquals("User profile data is missing. Please contact administrator.", exception.getMessage());
            verify(authRepo).updateLastLogin(200);
            verify(sessionManager, never()).startSession(any(User.class));
        }
    }

    @Test
    @DisplayName("Change Password: Success")
    void changePassword_Success() throws AuthException {
        int userId = 100;
        String username = "stu1";
        String oldPassword = "oldpass";
        String newPassword = "newpass123";
        String oldHashedPassword = PasswordHasher.hash(oldPassword);
        String newHashedPassword = PasswordHasher.hash(newPassword);

        UserAuthData authData = new UserAuthData(userId, Role.Student, oldHashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));
        when(authRepo.updatePasswordHash(eq(userId), anyString())).thenReturn(true);

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(oldPassword, oldHashedPassword)).thenReturn(true);
            mockedHasher.when(() -> PasswordHasher.hash(newPassword)).thenReturn(newHashedPassword);

            assertDoesNotThrow(() -> {
                authService.changePassword(userId, username, oldPassword, newPassword);
            });

            verify(authRepo).updatePasswordHash(eq(userId), eq(newHashedPassword));
        }
    }

    @Test
    @DisplayName("Change Password: Fail - Incorrect Old Password")
    void changePassword_Fail_IncorrectOldPassword() {
        int userId = 100;
        String username = "stu1";
        String oldPassword = "wrongoldpass";
        String newPassword = "newpass123";
        String actualHashedPassword = PasswordHasher.hash("correctoldpass");

        UserAuthData authData = new UserAuthData(userId, Role.Student, actualHashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(oldPassword, actualHashedPassword)).thenReturn(false);

            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.changePassword(userId, username, oldPassword, newPassword);
            });

            assertEquals("Incorrect current password.", exception.getMessage());
            verify(authRepo, never()).updatePasswordHash(anyInt(), anyString());
        }
    }

    @Test
    @DisplayName("Change Password: Fail - New Password Too Short")
    void changePassword_Fail_NewPasswordTooShort() {
        int userId = 100;
        String username = "stu1";
        String oldPassword = "oldpass";
        String newPassword = "123"; // Less than 6 characters

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.changePassword(userId, username, oldPassword, newPassword);
        });

        assertEquals("New password must be at least 6 characters long.", exception.getMessage());
        verify(authRepo, never()).findUserAuthDataByUsername(anyString());
        verify(authRepo, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    @DisplayName("Change Password: Fail - New Password Same As Old")
    void changePassword_Fail_NewPasswordSameAsOld() {
        int userId = 100;
        String username = "stu1";
        String password = "samepass123";

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.changePassword(userId, username, password, password);
        });

        assertEquals("New password cannot be the same as the old password.", exception.getMessage());
        verify(authRepo, never()).findUserAuthDataByUsername(anyString());
        verify(authRepo, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    @DisplayName("Change Password: Fail - User ID Mismatch (Security Check)")
    void changePassword_Fail_UserIdMismatch() {
        int sessionUserId = 100;
        int actualUserId = 200; // Different user ID
        String username = "stu1";
        String oldPassword = "oldpass";
        String newPassword = "newpass123";

        UserAuthData authData = new UserAuthData(actualUserId, Role.Student, "hashedpass");

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.changePassword(sessionUserId, username, oldPassword, newPassword);
        });

        assertEquals("Authentication error during password change.", exception.getMessage());
        verify(authRepo, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    @DisplayName("Change Password: Fail - User Not Found")
    void changePassword_Fail_UserNotFound() {
        int userId = 100;
        String username = "nonexistent";
        String oldPassword = "oldpass";
        String newPassword = "newpass123";

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.changePassword(userId, username, oldPassword, newPassword);
        });

        assertEquals("Authentication error during password change.", exception.getMessage());
        verify(authRepo, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    @DisplayName("Change Password: Fail - Database Update Fails")
    void changePassword_Fail_DatabaseUpdateFails() {
        int userId = 100;
        String username = "stu1";
        String oldPassword = "oldpass";
        String newPassword = "newpass123";
        String oldHashedPassword = PasswordHasher.hash(oldPassword);

        UserAuthData authData = new UserAuthData(userId, Role.Student, oldHashedPassword);

        when(authRepo.findUserAuthDataByUsername(username)).thenReturn(Optional.of(authData));
        when(authRepo.updatePasswordHash(eq(userId), anyString())).thenReturn(false);

        try (MockedStatic<PasswordHasher> mockedHasher = mockStatic(PasswordHasher.class)) {
            mockedHasher.when(() -> PasswordHasher.checkPassword(oldPassword, oldHashedPassword)).thenReturn(true);
            mockedHasher.when(() -> PasswordHasher.hash(newPassword)).thenReturn("newhash");

            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.changePassword(userId, username, oldPassword, newPassword);
            });

            assertEquals("Failed to update password. Please try again.", exception.getMessage());
        }
    }
}