package edu.univ.erp;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.RegistrationException;
import edu.univ.erp.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    private StudentService studentService;

    // Mocks for all dependencies
    @Mock private EnrollmentRepository enrollmentRepo;
    @Mock private SectionRepository sectionRepo;
    @Mock private SettingsRepository settingsRepo;
    @Mock private AccessControl accessControl;
    @Mock private CourseRepository courseRepo; 
    @Mock private GradeRepository gradeRepo;
    @Mock private InstructorProfileRepository instructorRepo;

    @BeforeEach
    void setUp() throws Exception {
        studentService = new StudentService();

        injectMock(studentService, "enrollmentRepo", enrollmentRepo);
        injectMock(studentService, "sectionRepo", sectionRepo);
        injectMock(studentService, "settingsRepo", settingsRepo);
        injectMock(studentService, "accessControl", accessControl);
        injectMock(studentService, "courseRepo", courseRepo);
        injectMock(studentService, "gradeRepo", gradeRepo);
        injectMock(studentService, "instructorRepo", instructorRepo);
    }

    // Helper to inject mocks into private final fields.
    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    @Test
    @DisplayName("Register: Should fail if Maintenance Mode is ON")
    void register_Fail_Maintenance() {
        when(accessControl.isMaintenanceModeOn()).thenReturn(true);

        assertThrows(RegistrationException.class, () -> {
            studentService.registerForSection(1, 101);
        }, "Should throw exception when maintenance is ON");

        verify(enrollmentRepo, never()).create(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Register: Should fail if Registration Deadline passed")
    void register_Fail_DeadlinePassed() {
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);
        // Set deadline to yesterday
        when(settingsRepo.getRegistrationDeadline()).thenReturn(Optional.of(LocalDate.now().minusDays(1)));

        assertThrows(RegistrationException.class, () -> {
            studentService.registerForSection(1, 101);
        });
    }

    @Test
    @DisplayName("Register: Should fail if Section is Full")
    void register_Fail_SectionFull() {
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);
        when(settingsRepo.getRegistrationDeadline()).thenReturn(Optional.of(LocalDate.now().plusDays(1)));
        when(enrollmentRepo.exists(1, 101)).thenReturn(false);

        // Section with capacity 30
        Section fullSection = new Section(101, 500, 2, "Mon", "Room 1", 30, "Fall", 2025);
        when(sectionRepo.findById(101)).thenReturn(Optional.of(fullSection));
        
        // Current enrollment is 30
        when(sectionRepo.getEnrollmentCount(101)).thenReturn(30);

        assertThrows(RegistrationException.class, () -> {
            studentService.registerForSection(1, 101);
        }, "Should throw exception when section is full");
    }

    @Test
    @DisplayName("Register: Success Scenario")
    void register_Success() throws RegistrationException {
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);
        when(settingsRepo.getRegistrationDeadline()).thenReturn(Optional.of(LocalDate.now().plusDays(1)));
        when(enrollmentRepo.exists(1, 101)).thenReturn(false);

        // Section with capacity 30, currently has 29
        Section section = new Section(101, 500, 2, "Mon", "Room 1", 30, "Monsoon", 2025);
        when(sectionRepo.findById(101)).thenReturn(Optional.of(section));
        when(sectionRepo.getEnrollmentCount(101)).thenReturn(29);
        
        when(enrollmentRepo.create(1, 101)).thenReturn(true);

        studentService.registerForSection(1, 101);
        verify(enrollmentRepo, times(1)).create(1, 101);
    }
}