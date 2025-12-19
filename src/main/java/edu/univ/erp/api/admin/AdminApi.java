package edu.univ.erp.api.admin;

import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.types.UserCreationRequest;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.AdminException;
import edu.univ.erp.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminApi {

    private static final Logger log = LoggerFactory.getLogger(AdminApi.class);
    private final AdminService adminService = new AdminService();

    // User Management
    public ApiResponse<Void> createUser(UserCreationRequest req) {
        try {
            adminService.createUser(req.username(), req.password(), req.role(), req.name(),
                    req.rollNo(), req.program(), req.year(), req.department());
            return ApiResponse.success(null, "User '" + req.username() + "' created successfully.");
        } catch (AdminException e) {
            log.warn("API: User creation failed: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Error creating user {}", req.username(), e);
            return ApiResponse.error("An Error occurred during user creation.");
        }
    }

    // Data Fetching for UI Dropdowns
    public ApiResponse<List<Course>> getAllCourses() {
        try {
            return ApiResponse.success(adminService.getAllCourses(), "Courses loaded.");
        } catch (Exception e) {
            log.error("API Error fetching all courses", e);
            return ApiResponse.error("Could not load courses.");
        }
    }

    public ApiResponse<List<Instructor>> getAllInstructors() {
        try {
            return ApiResponse.success(adminService.getAllInstructors(), "Instructors loaded.");
        } catch (Exception e) {
            log.error("API Error fetching all instructors", e);
            return ApiResponse.error("Could not load instructors.");
        }
    }

    // Course & Section Management
    public ApiResponse<Void> createCourse(String code, String title, int credits) {
        try {
            adminService.createCourse(code, title, credits);
            return ApiResponse.success(null, "Course '" + code + "' created successfully.");
        } catch (AdminException e) {
            log.warn("API: Course creation failed: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Error creating course {}", code, e);
            return ApiResponse.error("An Error occurred during course creation.");
        }
    }

    public ApiResponse<Void> createSection(int courseId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) {
        try {
            adminService.createSection(courseId, instructorId, dayTime, room, capacity, semester, year);
            return ApiResponse.success(null, "Section created successfully for course ID " + courseId);
        } catch (AdminException e) {
            log.warn("API: Section creation failed: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Error creating section for courseId {}", courseId, e);
            return ApiResponse.error("An Error occurred during section creation.");
        }
    }

    public ApiResponse<Void> assignInstructor(int sectionId, Integer instructorId) {
        try {
            adminService.assignInstructor(sectionId, instructorId);
            String msg = (instructorId == null) ? "unassigned from" : "assigned to";
            return ApiResponse.success(null, "Instructor successfully " + msg + " section " + sectionId);
        } catch (AdminException e) {
            log.warn("API: Assign instructor failed: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Error assigning instructor to section {}", sectionId, e);
            return ApiResponse.error("An Error occurred during assignment.");
        }
    }

    public ApiResponse<Void> deleteSection(int sectionId) {
        try {
            adminService.deleteSection(sectionId);

            log.info("API: Section {} deleted.", sectionId);
            return ApiResponse.success(null, "Section " + sectionId + " deleted successfully.");

        } catch (AdminException e) {
            log.warn("API: Section deletion failed for ID {}: {}", sectionId, e.getMessage());
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            log.error("API: Error deleting section {}", sectionId, e);
            return ApiResponse.error("An Error occurred during section deletion.");
        }
    }

    public ApiResponse<Void> editCourse(int courseId, String newTitle, int newCredits) {
        try {
            adminService.updateCourse(courseId, newTitle, newCredits);
            log.info("API: Course {} updated.", courseId);
            return ApiResponse.success(null, "Course updated successfully.");
        } catch (AdminException e) {
            log.warn("API: Update course failed for ID {}: {}", courseId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Error updating course {}", courseId, e);
            return ApiResponse.error("An Error occurred while updating the course.");
        }
    }

    public ApiResponse<Void> editSection(int sectionId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) {
        try {
            adminService.updateSection(sectionId, instructorId, dayTime, room, capacity, semester, year);
            log.info("API: Section {} updated.", sectionId);
            return ApiResponse.success(null, "Section updated successfully.");
        } catch (AdminException e) {
            log.warn("API: Update section failed for ID {}: {}", sectionId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Error updating section {}", sectionId, e);
            return ApiResponse.error("An Error occurred while updating the section.");
        }
    }

    public ApiResponse<List<Section>> getSectionsByCourse(int courseId) {
        try {
            List<Section> list = adminService.getSectionsByCourse(courseId);
            return ApiResponse.success(list, "Sections loaded.");
        } catch (AdminException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Error loading sections.");
        }
    }
}