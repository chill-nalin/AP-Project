package edu.univ.erp;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.GradeException;
import edu.univ.erp.service.InstructorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorServiceTest {

    private InstructorService instructorService;

    @Mock private GradeRepository gradeRepo;
    @Mock private EnrollmentRepository enrollmentRepo;
    @Mock private AccessControl accessControl;
    @Mock private SectionRepository sectionRepo;
    @Mock private CourseRepository courseRepo;
    @Mock private StudentProfileRepository studentRepo;
    @Mock private SettingsRepository settingsRepo;

    @BeforeEach
    void setUp() throws Exception {
        instructorService = new InstructorService();
        
        injectMock(instructorService, "gradeRepo", gradeRepo);
        injectMock(instructorService, "enrollmentRepo", enrollmentRepo);
        injectMock(instructorService, "accessControl", accessControl);
        injectMock(instructorService, "sectionRepo", sectionRepo);
        injectMock(instructorService, "courseRepo", courseRepo);
        injectMock(instructorService, "studentRepo", studentRepo);
        injectMock(instructorService, "settingsRepo", settingsRepo);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    @Test
    @DisplayName("Compute Final Grades: Calculation Logic (Summation)")
    void computeFinalGrades_Success() throws GradeException {
        int sectionId = 201;
        User instructor = new User(2, "inst1", Role.Instructor, null);
        
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);
        when(accessControl.canInstructorGradeSection(instructor, sectionId)).thenReturn(true);

        // Mock one student enrollment
        Enrollment enrollment = new Enrollment(1001, 50, sectionId, "Enrolled");
        when(enrollmentRepo.findBySectionId(sectionId)).thenReturn(List.of(enrollment));

        // Mock grades for that student
        // Quiz: 20, Midterm: 30, EndSem: 40 => Total 90
        List<Grade> grades = List.of(
            new Grade(1, 1001, "Quiz", 20.0, null),
            new Grade(2, 1001, "Midterm", 30.0, null),
            new Grade(3, 1001, "EndSem", 40.0, null)
        );
        when(gradeRepo.findByEnrollmentId(1001)).thenReturn(grades);
        when(gradeRepo.updateFinalGradeForEnrollment(anyInt(), anyString())).thenReturn(true);

        // Boundaries: A+ > 80, A > 70, ...
        List<Double> boundaries = List.of(80.0, 70.0, 60.0, 50.0, 40.0);

        instructorService.computeFinalGrades(instructor, sectionId, boundaries);

        // Total is 90. 90 >= 80 (Boundaries[0]), so grade should be "A+"
        verify(gradeRepo).updateFinalGradeForEnrollment(1001, "A+");
    }

    @Test
    @DisplayName("Compute Final Grades: Fails if score > 100")
    void computeFinalGrades_Fail_ScoreTooHigh() {
        int sectionId = 201;
        User instructor = new User(2, "inst1", Role.Instructor, null);
        
        when(accessControl.isMaintenanceModeOn()).thenReturn(false);
        when(accessControl.canInstructorGradeSection(instructor, sectionId)).thenReturn(true);

        Enrollment enrollment = new Enrollment(1001, 50, sectionId, "Enrolled");
        when(enrollmentRepo.findBySectionId(sectionId)).thenReturn(List.of(enrollment));

        // Total = 110
        List<Grade> grades = List.of(
            new Grade(1, 1001, "Quiz", 50.0, null),
            new Grade(2, 1001, "Midterm", 30.0, null),
            new Grade(3, 1001, "EndSem", 30.0, null)
        );
        when(gradeRepo.findByEnrollmentId(1001)).thenReturn(grades);

        List<Double> boundaries = List.of(80.0, 70.0, 60.0, 50.0, 40.0);

        // Expecting error because total > 100
        assertThrows(GradeException.class, () -> {
            instructorService.computeFinalGrades(instructor, sectionId, boundaries);
        });
    }
}