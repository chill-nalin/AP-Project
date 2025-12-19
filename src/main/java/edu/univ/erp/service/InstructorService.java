package edu.univ.erp.service;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.api.types.GradebookRow;
import edu.univ.erp.api.types.InstructorSectionRow;
import edu.univ.erp.api.types.ScoreEntryRequest;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import edu.univ.erp.ui.component.GradebookPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class InstructorService {

    private static final Logger log = LoggerFactory.getLogger(InstructorService.class);

    private final SectionRepository sectionRepo = new SectionRepository();
    private final CourseRepository courseRepo = new CourseRepository();
    private final EnrollmentRepository enrollmentRepo = new EnrollmentRepository();
    private final GradeRepository gradeRepo = new GradeRepository();
    private final StudentProfileRepository studentRepo = new StudentProfileRepository();
    private final AccessControl accessControl = new AccessControl();
    private final SettingsRepository settingsRepo = new SettingsRepository();


    public List<InstructorSectionRow> getMySections(int instructorId) {
        String semester = settingsRepo.getCurrentSemester();
        int year = settingsRepo.getCurrentYear();
        log.debug("Fetching sections for instructor {} in {}-{}", instructorId, semester, year);
        List<InstructorSectionRow> sectionRows = new ArrayList<>();
        List<Section> sections = sectionRepo.findByInstructorIdAndTerm(instructorId, semester, year);

        for (Section section : sections) {
            Optional<Course> courseOpt = courseRepo.findById(section.courseId());
            if (courseOpt.isEmpty()) {
                log.warn("Skipping section {}: Course {} not found.", section.sectionId(), section.courseId());
                continue;
            }
            Course course = courseOpt.get();
            int enrolledCount = sectionRepo.getEnrollmentCount(section.sectionId());

            InstructorSectionRow row = new InstructorSectionRow(
                    section.sectionId(),
                    course.code(),
                    course.title(),
                    section.dayTime(),
                    section.room(),
                    enrolledCount,
                    section.capacity()
            );
            sectionRows.add(row);
        }
        log.info("Found {} sections for instructor {}", sectionRows.size(), instructorId);
        return sectionRows;
    }

    public List<GradebookRow> getGradebookForSection(int sectionId) {
        log.debug("Fetching gradebook for section {}", sectionId);
        List<GradebookRow> gradebook = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepo.findBySectionId(sectionId);

        if (enrollments.isEmpty()) {
            log.info("No enrollments found for section {}.", sectionId);
            return gradebook;
        }

        for (Enrollment enrollment : enrollments) {
            Optional<Student> studentOpt = studentRepo.findById(enrollment.studentId());
            String rollNo = studentOpt.map(Student::rollNo).orElse("Unknown");
            log.debug("Processing enrollment ID: {}, Student ID: {}, RollNo: {}", enrollment.enrollmentId(), enrollment.studentId(), rollNo);

            List<Grade> grades = gradeRepo.findByEnrollmentId(enrollment.enrollmentId());
            log.debug("Found {} grade records for enrollment ID {}", grades.size(), enrollment.enrollmentId());

            Map<String, Double> scores = grades.stream()
                    .filter(g -> g.score() != null && g.component() != null)
                    .collect(Collectors.toMap(
                            Grade::component,
                            Grade::score,
                            (existingScore, newScore) -> {
                                log.warn("Duplicate score found for enrollment {}. Using first score encountered: {}",
                                        enrollment.enrollmentId(), existingScore);
                                return existingScore;
                            }
                    ));
            log.debug("Scores map for enrollment {}: {}", enrollment.enrollmentId(), scores);

            String finalGradeStr = grades.stream()
                    .map(Grade::finalGrade)
                    .filter(fg -> fg != null && !fg.isBlank())
                    .findFirst()
                    .orElse(null);
            log.debug("Final grade string for enrollment {}: {}", enrollment.enrollmentId(), finalGradeStr);

            GradebookRow row = new GradebookRow(
                    enrollment.enrollmentId(),
                    enrollment.studentId(),
                    rollNo,
                    scores.get(GradebookPanel.QUIZ),
                    scores.get(GradebookPanel.MIDTERM),
                    scores.get(GradebookPanel.ENDSEM),
                    finalGradeStr
            );
            gradebook.add(row);
        }
        log.info("Gradebook construction complete for section {}. Rows: {}", sectionId, gradebook.size());
        return gradebook;
    }


    public void enterScore(User instructor, ScoreEntryRequest request) throws GradeException {
        log.info("Attempting score entry by instructor {}: {}", instructor.userId(), request);
        if (accessControl.isMaintenanceModeOn()) {
            throw new GradeException("Grading is currently disabled due to system maintenance.");
        }
        Optional<Enrollment> enrollOpt = enrollmentRepo.findById(request.enrollmentId());
        if (enrollOpt.isEmpty()) {
            log.error("Score entry failed: Enrollment {} not found.", request.enrollmentId());
            throw new GradeException("Enrollment record not found.");
        }
        int sectionId = enrollOpt.get().sectionId();
        if (!accessControl.canInstructorGradeSection(instructor, sectionId)) {
            throw new GradeException("You are not authorized to enter grades for this section.");
        }
        if (request.score() != null && (request.score() < 0 || request.score() > 100)) {
            throw new GradeException("Score must be between 0 and 100 (or empty).");
        }
        if (!gradeRepo.saveOrUpdateScore(request.enrollmentId(), request.component(), request.score())) {
            throw new GradeException("Could not save the score due to a database error.");
        }
        log.info("Score entry successful.");
    }

    public void computeFinalGrades(User instructor, int sectionId, List<Double> gradeBoundaries) throws GradeException {
        log.info("Attempting final grade computation (SUM method) for section {} by instructor {} using boundaries: {}",
                sectionId, instructor.userId(), gradeBoundaries);

        // Initial Checks
        if (accessControl.isMaintenanceModeOn()) { throw new GradeException("Grading disabled due to maintenance."); }
        if (!accessControl.canInstructorGradeSection(instructor, sectionId)) { throw new GradeException("Not authorized for this section."); }
        // Validate boundaries (size, descending, range 0-100)
        if (gradeBoundaries == null || gradeBoundaries.size() != 5) throw new GradeException("Internal Error: Invalid number of grade boundaries.");
        for (int i = 0; i < gradeBoundaries.size() - 1; i++) {
            if (gradeBoundaries.get(i) <= gradeBoundaries.get(i + 1)) throw new GradeException("Internal Error: Boundaries not descending.");
        }
        for(Double boundary : gradeBoundaries) {
            if(boundary < 0 || boundary > 100) throw new GradeException("Internal Error: Boundaries out of range 0-100.");
        }

        // Processing Enrollments
        List<Enrollment> enrollments = enrollmentRepo.findBySectionId(sectionId);
        if (enrollments.isEmpty()) { log.info("No students enrolled in section {}.", sectionId); return; }

        int successCount = 0;
        int failCount = 0;
        int incompleteCount = 0;

        final String quizComp = GradebookPanel.QUIZ;
        final String midtermComp = GradebookPanel.MIDTERM;
        final String endSemComp = GradebookPanel.ENDSEM;
        final List<String> requiredComponents = List.of(quizComp, midtermComp, endSemComp);
        final double maxPossibleScore = 100.0;

        for (Enrollment enrollment : enrollments) {
            try {
                List<Grade> grades = gradeRepo.findByEnrollmentId(enrollment.enrollmentId());
                Map<String, Double> scores = grades.stream()
                        .filter(g -> g.score() != null && g.component() != null)
                        .collect(Collectors.toMap(Grade::component, Grade::score, (s1, s2) -> s1));
                log.debug("Calculating final grade for enrollment {}. Scores: {}", enrollment.enrollmentId(), scores);

                boolean missingComponent = false;
                for (String required : requiredComponents) {
                    if (!scores.containsKey(required)) {
                        log.warn("Skipping final grade for enrollment {}: Missing required score for component '{}'.", enrollment.enrollmentId(), required);
                        missingComponent = true;
                        break;
                    }
                }

                if (missingComponent) {
                    boolean savedIncomplete = gradeRepo.updateFinalGradeForEnrollment(enrollment.enrollmentId(), "I");
                    if (savedIncomplete) incompleteCount++; else failCount++;
                    continue;
                }

                double finalNumericScore = scores.get(quizComp) + scores.get(midtermComp) + scores.get(endSemComp);
                log.debug("Calculated raw score sum: {:.2f} for enrollment {}", finalNumericScore, enrollment.enrollmentId());

                if (finalNumericScore > maxPossibleScore) {
                    log.error("Final score calculation error for enrollment {}: Score sum ({:.2f}) exceeds {}.", enrollment.enrollmentId(), finalNumericScore, maxPossibleScore);
                    throw new GradeException("Calculated score sum ("+String.format("%.2f", finalNumericScore)+") exceeds "+maxPossibleScore+" for student with enrollment ID " + enrollment.enrollmentId() + ". Please check component scores.");
                } else if (finalNumericScore < 0) {
                    log.warn("Calculated score sum ({:.2f}) is negative for enrollment {}. Clamping to 0.", finalNumericScore, enrollment.enrollmentId());
                    finalNumericScore = 0.0;
                }
                // Determine letter grade using the given boundaries
                String finalLetterGrade = calculateLetterGrade(finalNumericScore, gradeBoundaries);
                log.debug("Final score used for grading: {:.2f}, Letter Grade: {} for enrollment {}", finalNumericScore, finalLetterGrade, enrollment.enrollmentId());

                boolean saved = gradeRepo.updateFinalGradeForEnrollment(enrollment.enrollmentId(), finalLetterGrade);
                if (saved) successCount++; else failCount++;

            } catch (GradeException ge) { // Catch calculation errors (like score > 100)
                log.error("Grade computation error for enrollment {}: {}", enrollment.enrollmentId(), ge.getMessage());
                failCount++;
            }
            catch (Exception e) {
                log.error("Unexpected error computing final grade for enrollment {}", enrollment.enrollmentId(), e);
                failCount++;
            }
        }

        log.info("Final grade computation complete for section {}. Success: {}, Failed: {}, Incomplete: {}", sectionId, successCount, failCount, incompleteCount);
        if (failCount > 0) {
            throw new GradeException("Failed to compute or save final grades for " + failCount + " student(s) due to errors (e.g., score sum > "+maxPossibleScore+"). Check logs.");
        }
    }

    private String calculateLetterGrade(double score, List<Double> boundaries) {
        log.debug("Calculating grade for score: {:.2f} using boundaries: {}", score, boundaries);
        if (score >= boundaries.get(0)) { log.debug("Score >= {} (A+), returning A+", boundaries.get(0)); return "A+"; }
        if (score >= boundaries.get(1)) { log.debug("Score >= {} (A), returning A", boundaries.get(1)); return "A"; }
        if (score >= boundaries.get(2)) { log.debug("Score >= {} (B), returning B", boundaries.get(2)); return "B"; }
        if (score >= boundaries.get(3)) { log.debug("Score >= {} (C), returning C", boundaries.get(3)); return "C"; }
        if (score >= boundaries.get(4)) { log.debug("Score >= {} (D), returning D", boundaries.get(4)); return "D"; }
        log.debug("Score < {} (D), returning F", boundaries.get(4));
        return "F";
    }

    public Map<String, Double> getSectionStatistics(int sectionId) {
        log.debug("Calculating statistics for section {}", sectionId);
        Map<String, Double> averageScores = new HashMap<>();
        Map<String, List<Double>> scoresByComponent = new HashMap<>();
        List<Enrollment> enrollments = enrollmentRepo.findBySectionId(sectionId);
        if (enrollments.isEmpty()) {
            log.info("No enrollments for section {}. Cannot calculate stats.", sectionId);
            return averageScores;
        }
        for (Enrollment enrollment : enrollments) {
            List<Grade> grades = gradeRepo.findByEnrollmentId(enrollment.enrollmentId());
            for (Grade grade : grades) {
                if (grade.component() != null && grade.score() != null) {
                    scoresByComponent.computeIfAbsent(grade.component(), k -> new ArrayList<>()).add(grade.score());
                }
            }
        }
        for (Map.Entry<String, List<Double>> entry : scoresByComponent.entrySet()) {
            String component = entry.getKey();
            List<Double> scores = entry.getValue();
            if (!scores.isEmpty()) {
                double sum = scores.stream().mapToDouble(Double::doubleValue).sum();
                double average = sum / scores.size();
                averageScores.put(component, average);
                log.debug("Section {}: Average for '{}' is {:.2f} ({} scores)", sectionId, component, average, scores.size());
            }
        }
        log.info("Calculated statistics for {} components in section {}", averageScores.size(), sectionId);
        return averageScores;
    }
}