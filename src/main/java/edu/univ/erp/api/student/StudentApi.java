package edu.univ.erp.api.student;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.types.GradeRow;
import edu.univ.erp.api.types.RegistrationRow;
import edu.univ.erp.api.types.TimetableEntry;
import edu.univ.erp.service.DropException;
import edu.univ.erp.service.RegistrationException;
import edu.univ.erp.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StudentApi {

    private static final Logger log = LoggerFactory.getLogger(StudentApi.class);
    private final StudentService studentService = new StudentService();

    public ApiResponse<Void> registerForSection(int studentId, int sectionId) {
        try {
            studentService.registerForSection(studentId, sectionId);
            log.info("API: Registration successful for student {} in section {}", studentId, sectionId);
            return ApiResponse.success(null, "Successfully registered for the section!");
        } catch (RegistrationException e) {
            log.warn("API: Registration failed for student {} in section {}: {}", studentId, sectionId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error during registration for student {} in section {}", studentId, sectionId, e);
            return ApiResponse.error("An unexpected error occurred. Please contact support.");
        }
    }

    public ApiResponse<List<RegistrationRow>> getMyRegistrations(int studentId) {
        try {
            List<RegistrationRow> registrations = studentService.getMyRegistrations(studentId);
            log.info("API: Fetched {} registrations for student {}", registrations.size(), studentId);
            return ApiResponse.success(registrations, "Registrations loaded.");
        } catch (Exception e) {
            log.error("API: Unexpected error fetching registrations for student {}", studentId, e);
            return ApiResponse.error("Could not load your registrations. Please try again later.");
        }
    }

    public ApiResponse<Void> dropSection(int studentId, int enrollmentId) {
        try {
            studentService.dropSection(studentId, enrollmentId);
            log.info("API: Drop successful for enrollment {} by student {}", enrollmentId, studentId);
            return ApiResponse.success(null, "Section dropped successfully!");
        } catch (DropException e) {
            log.warn("API: Drop failed for enrollment {} by student {}: {}", enrollmentId, studentId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error during drop for enrollment {} by student {}", enrollmentId, studentId, e);
            return ApiResponse.error("An unexpected error occurred. Please contact support.");
        }
    }

    public ApiResponse<List<GradeRow>> getMyGrades(int studentId) {
        try {
            List<GradeRow> grades = studentService.getMyGrades(studentId);
            log.info("API: Fetched {} grade rows for student {}", grades.size(), studentId);
            return ApiResponse.success(grades, "Grades loaded successfully.");
        } catch (Exception e) {
            log.error("API: Unexpected error fetching grades for student {}", studentId, e);
            return ApiResponse.error("Could not load your grades. Please try again later.");
        }
    }

    public ApiResponse<List<TimetableEntry>> getMyTimetable(int studentId) {
        try {
            List<TimetableEntry> timetable = studentService.getMyTimetable(studentId);
            log.info("API: Fetched {} timetable entries for student {}", timetable.size(), studentId);
            return ApiResponse.success(timetable, "Timetable loaded successfully.");
        } catch (Exception e) {
            log.error("API: Unexpected error fetching timetable for student {}", studentId, e);
            return ApiResponse.error("Could not load your timetable. Please try again later.");
        }
    }
}