package edu.univ.erp.service;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.auth.PasswordHasher;
import edu.univ.erp.auth.UserAuthRepository;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserAuthRepository authRepo = new UserAuthRepository();
    private final StudentProfileRepository studentRepo = new StudentProfileRepository();
    private final InstructorProfileRepository instructorRepo = new InstructorProfileRepository();
    private final CourseRepository courseRepo = new CourseRepository();
    private final SectionRepository sectionRepo = new SectionRepository();
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final EnrollmentRepository enrollmentRepo = new EnrollmentRepository();
    private final AccessControl accessControl = new AccessControl();

    private void blockIfMaintenance() throws AdminException {
        if (accessControl.isMaintenanceModeOn()) {
            throw new AdminException("Action is disabled while system is in maintenance (read-only) mode.");
        }
    }

    // User Management
    public void createUser(String username, String password, Role role, String name, String rollNo, String program, int year, String department) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to create user: username={}, role={}", username, role);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AdminException("Username and password cannot be empty.");
        }

        String hashedPassword = PasswordHasher.hash(password);

        Optional<Integer> newUserIdOpt = authRepo.createUserAuth(username, hashedPassword, role);
        if (newUserIdOpt.isEmpty()) {
            throw new AdminException("Failed to create user login. Username might already exist.");
        }
        int newUserId = newUserIdOpt.get();

        boolean profileCreated = false;
        try {
            switch (role) {
                case Student:
                    if (rollNo == null || rollNo.isBlank())
                        throw new AdminException("Roll number is required for students.");
                    profileCreated = studentRepo.createStudent(newUserId, rollNo, program, year);
                    break;
                case Instructor:
                    if (name == null || name.isBlank())
                        throw new AdminException("Name is required for instructors.");
                    profileCreated = instructorRepo.createInstructor(newUserId, name, department);
                    break;
                case Admin:
                    profileCreated = true;
                    break;
            }
        } catch (Exception e) {
            log.error("Error creating profile for user {}, attempting rollback of auth record.", newUserId, e);
            boolean rollbackSuccess = authRepo.deleteUserAuthById(newUserId);
            if (!rollbackSuccess) {
                log.error("CRITICAL: Failed to rollback auth for user {}", newUserId);
            }
            throw new AdminException("Failed to create user profile after creating login.", e);
        }

        if (!profileCreated) {
            log.error("Profile creation failed for user {}, attempting rollback.", newUserId);
            boolean rollbackSuccess = authRepo.deleteUserAuthById(newUserId);
            if (!rollbackSuccess) {
                log.error("CRITICAL: Failed to rollback auth for user {}", newUserId);
            }
            throw new AdminException("Failed to create user profile.");
        }

        log.info("Successfully created user {} with user_id {}", username, newUserId);
    }

    // Course & Section Read API
    public List<Course> getAllCourses() { return courseRepo.findAll(); }
    public List<Instructor> getAllInstructors() { return instructorRepo.findAll(); }
    public List<Section> getSectionsByCourse(int courseId) throws AdminException {
        return sectionRepo.findSectionsByCourse(courseId);
    }

    // Course & Section Write API
    public void createCourse(String code, String title, int credits) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to create course: code={}, title={}", code, title);
        if (code == null || code.isBlank() || title == null || title.isBlank()) {
            throw new AdminException("Course code and title cannot be empty.");
        }
        if (credits <= 0) throw new AdminException("Credits must be positive.");
        if (credits > 4) throw new AdminException("Credits cannot be more than 4");

        if (!courseRepo.createCourse(code, title, credits)) {
            throw new AdminException("Failed to create course. Code might already exist.");
        }
        log.info("Successfully created course {}", code);
    }

    public void updateCourse(int courseId, String newTitle, int newCredits) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to update course {}", courseId);
        if (newTitle == null || newTitle.isBlank())
            throw new AdminException("Course title cannot be empty.");
        if (newCredits <= 0)
            throw new AdminException("Credits must be positive.");
        if (newCredits > 4)
            throw new AdminException("Credits cannot be more than 4");

        if (!courseRepo.updateCourse(courseId, newTitle.trim(), newCredits)) {
            throw new AdminException("Failed to update course.");
        }
        log.info("Successfully updated course {}", courseId);
    }

    public void createSection(int courseId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to create section for courseId {}", courseId);
        if (capacity <= 0) throw new AdminException("Capacity must be positive.");
        if (semester == null || semester.isBlank()) throw new AdminException("Semester cannot be empty.");
        if (year < 2025 || year > 2026)
            throw new AdminException("Invalid year. Allowed: 2025 or 2026");

        if (!sectionRepo.createSection(courseId, instructorId, dayTime, room, capacity, semester, year)) {
            throw new AdminException("Failed to create section.");
        }
        log.info("Successfully created section");
    }

    public void updateSection(int sectionId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to update section {}", sectionId);
        if (capacity <= 0) throw new AdminException("Capacity must be positive.");
        if (semester == null || semester.isBlank()) throw new AdminException("Semester cannot be empty.");
        if (year < 2025 || year > 2026)
            throw new AdminException("Invalid year. Allowed: 2025 or 2026");

        int enrolledCount = enrollmentRepo.countEnrollmentsBySection(sectionId);
        if (enrolledCount > 0 && capacity < enrolledCount) {
            throw new AdminException("Cannot reduce capacity below enrolled count (" + enrolledCount + ").");
        }

        if (!sectionRepo.updateSection(sectionId, instructorId, dayTime, room, capacity, semester, year)) {
            throw new AdminException("Failed to update section.");
        }
        log.info("Successfully updated section {}", sectionId);
    }

    public void assignInstructor(int sectionId, Integer instructorId) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to assign instructor {} to section {}", instructorId, sectionId);
        if (!sectionRepo.updateInstructor(sectionId, instructorId)) {
            throw new AdminException("Failed to assign instructor.");
        }
        log.info("Successfully assigned instructor");
    }

    public void deleteSection(int sectionId) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to delete section {}", sectionId);
        int enrolledCount = enrollmentRepo.countEnrollmentsBySection(sectionId);
        if (enrolledCount > 0) {
            throw new AdminException("Cannot delete section — students are enrolled.");
        }
        if (!sectionRepo.deleteById(sectionId)) {
            throw new AdminException("Failed to delete section.");
        }
        log.info("Successfully deleted section {}", sectionId);
    }

    // Maintenance setters (NOT BLOCKED by maintenance check)

    public void setMaintenanceMode(boolean enabled) throws AdminException {
        log.info("Attempting to set maintenance mode to {}", enabled);
        if (!settingsRepo.setMaintenanceMode(enabled)) {
            throw new AdminException("Failed to update maintenance mode.");
        }
        log.info("Successfully set maintenance mode");
    }

    public void setDropDeadline(LocalDate deadline) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to set drop deadline to {}", deadline);
        if (deadline == null) throw new AdminException("Drop deadline cannot be null.");
        if (!settingsRepo.setDropDeadline(deadline)) {
            throw new AdminException("Failed to update drop deadline.");
        }
        log.info("Successfully set drop deadline");
    }

    public void setRegistrationDeadline(LocalDate deadline) throws AdminException {
        blockIfMaintenance();

        log.info("Attempting to set registration deadline to {}", deadline);
        if (deadline == null) throw new AdminException("Registration deadline cannot be null.");
        if (!settingsRepo.setRegistrationDeadline(deadline)) {
            throw new AdminException("Failed to update registration deadline.");
        }
        log.info("Successfully set registration deadline");
    }
}
