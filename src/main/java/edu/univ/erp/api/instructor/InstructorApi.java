package edu.univ.erp.api.instructor;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.api.common.ApiResponse;
import edu.univ.erp.api.types.GradebookRow;
import edu.univ.erp.api.types.InstructorSectionRow;
import edu.univ.erp.api.types.ScoreEntryRequest;
import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.GradeException;
import edu.univ.erp.service.InstructorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class InstructorApi {

    private static final Logger log = LoggerFactory.getLogger(InstructorApi.class);
    private final InstructorService instructorService = new InstructorService();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AccessControl accessControl = new AccessControl();

    public ApiResponse<List<InstructorSectionRow>> getMySections() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.role() != edu.univ.erp.domain.Role.Instructor) {
            return ApiResponse.error("Unauthorized access.");
        }
        try {
            List<InstructorSectionRow> sections = instructorService.getMySections(currentUser.userId());
            return ApiResponse.success(sections, "Sections loaded.");
        } catch (Exception e) {
            log.error("API Error fetching sections for instructor {}", currentUser.userId(), e);
            return ApiResponse.error("Could not load assigned sections.");
        }
    }

    public ApiResponse<List<GradebookRow>> getGradebook(int sectionId) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.role() != edu.univ.erp.domain.Role.Instructor) {
            return ApiResponse.error("Unauthorized access.");
        }
        if (!accessControl.canInstructorGradeSection(currentUser, sectionId)) {
            return ApiResponse.error("You are not assigned to this section.");
        }
        try {
            List<GradebookRow> gradebook = instructorService.getGradebookForSection(sectionId);
            return ApiResponse.success(gradebook, "Gradebook loaded.");
        } catch (Exception e) {
            log.error("API Error fetching gradebook for section {}", sectionId, e);
            return ApiResponse.error("Could not load gradebook.");
        }
    }

    public ApiResponse<Void> enterScore(ScoreEntryRequest request) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.role() != edu.univ.erp.domain.Role.Instructor) {
            return ApiResponse.error("Unauthorized access.");
        }
        try {
            instructorService.enterScore(currentUser, request);
            return ApiResponse.success(null, "Score saved successfully.");
        } catch (GradeException e) {
            log.warn("API: Score entry failed: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error entering score: {}", request, e);
            return ApiResponse.error("An unexpected error occurred while saving the score.");
        }
    }

    public ApiResponse<Void> computeFinalGrades(int sectionId, List<Double> gradeBoundaries) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.role() != edu.univ.erp.domain.Role.Instructor) return ApiResponse.error("Unauthorized access.");
        if (!accessControl.canInstructorGradeSection(currentUser, sectionId)) return ApiResponse.error("You are not assigned to this section.");

        try {
            instructorService.computeFinalGrades(currentUser, sectionId, gradeBoundaries);
            log.info("API: Final grade computation successful for section {}", sectionId);
            return ApiResponse.success(null, "Final grades computed and saved successfully based on provided boundaries.");
        } catch (GradeException e) {
            log.warn("API: Final grade computation failed for section {}: {}", sectionId, e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("API: Unexpected error computing final grades for section {}", sectionId, e);
            return ApiResponse.error("An unexpected error occurred during final grade computation.");
        }
    }

    public ApiResponse<Map<String, Double>> getSectionStatistics(int sectionId) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || currentUser.role() != edu.univ.erp.domain.Role.Instructor) {
            return ApiResponse.error("Unauthorized access.");
        }
        if (!accessControl.canInstructorGradeSection(currentUser, sectionId)) {
            return ApiResponse.error("You are not assigned to this section.");
        }

        try {
            Map<String, Double> stats = instructorService.getSectionStatistics(sectionId);
            return ApiResponse.success(stats, "Statistics loaded.");
        } catch (Exception e) {
            log.error("API Error fetching statistics for section {}", sectionId, e);
            return ApiResponse.error("Could not load statistics.");
        }
    }
}