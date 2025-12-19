package edu.univ.erp;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.auth.UserAuthRepository;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.Role;
import edu.univ.erp.service.AdminException;
import edu.univ.erp.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private AdminService adminService;

    @Mock private UserAuthRepository authRepo;
    @Mock private StudentProfileRepository studentRepo;
    @Mock private CourseRepository courseRepo;
    @Mock private AccessControl accessControl;
    @Mock private InstructorProfileRepository instructorRepo;
    @Mock private SectionRepository sectionRepo;
    @Mock private SettingsRepository settingsRepo;
    @Mock private EnrollmentRepository enrollmentRepo;

    @BeforeEach
    void setUp() throws Exception {
        adminService = new AdminService();
        injectMock(adminService, "authRepo", authRepo);
        injectMock(adminService, "studentRepo", studentRepo);
        injectMock(adminService, "courseRepo", courseRepo);
        injectMock(adminService, "accessControl", accessControl);
        injectMock(adminService, "instructorRepo", instructorRepo);
        injectMock(adminService, "sectionRepo", sectionRepo);
        injectMock(adminService, "settingsRepo", settingsRepo);
        injectMock(adminService, "enrollmentRepo", enrollmentRepo);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    @Test
    @DisplayName("Create User: Success Scenario")
    void createUser_Success() throws AdminException {
        when(authRepo.createUserAuth(anyString(), anyString(), eq(Role.Student))).thenReturn(Optional.of(100));
        when(studentRepo.createStudent(eq(100), anyString(), anyString(), anyInt())).thenReturn(true);
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);

        adminService.createUser("stu1", "pass", Role.Student, null, "Roll1", "CS", 1, null);

        verify(authRepo).createUserAuth(anyString(), anyString(), eq(Role.Student));
        verify(studentRepo).createStudent(eq(100), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Create User: Rollback if Profile Creation Fails")
    void createUser_Rollback() {
        when(authRepo.createUserAuth(anyString(), anyString(), eq(Role.Student))).thenReturn(Optional.of(100));
        when(studentRepo.createStudent(eq(100), anyString(), anyString(), anyInt())).thenReturn(false);
        
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);

        assertThrows(AdminException.class, () -> {
            adminService.createUser("stu1", "pass", Role.Student, null, "Roll1", "CS", 1, null);
        });

        verify(authRepo).deleteUserAuthById(100);
    }

    @Test
    @DisplayName("Create Course: Fails on Invalid Input")
    void createCourse_Fail_InvalidInput() {
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);

        assertThrows(AdminException.class, () -> {
            adminService.createCourse("", "Title", 4);
        });

        assertThrows(AdminException.class, () -> {
            adminService.createCourse("CS101", "Title", -1);
        });

        verify(courseRepo, never()).createCourse(anyString(), anyString(), anyInt());
    }
}