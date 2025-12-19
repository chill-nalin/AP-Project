package edu.univ.erp.service;

import edu.univ.erp.access.AccessControl;
import edu.univ.erp.api.types.GradeRow;
import edu.univ.erp.api.types.RegistrationRow;
import edu.univ.erp.api.types.TimetableEntry;
import edu.univ.erp.api.types.TranscriptEntry;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final EnrollmentRepository enrollmentRepo = new EnrollmentRepository();
    private final SectionRepository sectionRepo = new SectionRepository();
    private final CourseRepository courseRepo = new CourseRepository();
    private final InstructorProfileRepository instructorRepo = new InstructorProfileRepository();
    private final AccessControl accessControl = new AccessControl();
    private final GradeRepository gradeRepo = new GradeRepository();

    public void registerForSection(int studentId, int sectionId) throws RegistrationException {
        log.info("Attempting registration for student {} in section {}", studentId, sectionId);

        // 1. Check Maintenance Mode
        if (accessControl.isMaintenanceModeOn()) {
            throw new RegistrationException("Registration is currently disabled due to system maintenance.");
        }
        log.debug("Maintenance mode check passed.");

        // 2. Check Registration Deadline
        Optional<LocalDate> deadlineOpt = settingsRepo.getRegistrationDeadline();
        if (deadlineOpt.isEmpty()) {
            log.error("Registration blocked: Registration deadline is not configured in settings.");
            throw new RegistrationException("The registration deadline has not been set by the administrator.");
        }
        LocalDate deadline = deadlineOpt.get();
        LocalDate today = LocalDate.now();
        log.debug("Comparing current date ({}) with registration deadline ({}) for section {}", today, deadline, sectionId);
        if (today.isAfter(deadline)) {
            log.warn("Registration blocked: Deadline {} has passed for section {}.", deadline, sectionId);
            throw new RegistrationException("The deadline to register for courses (" + deadline + ") has passed.");
        }
        log.debug("Registration deadline check passed.");

        // 3. Check for Duplicate Enrollment in the same section
        if (enrollmentRepo.exists(studentId, sectionId)) {
            log.warn("Registration blocked: student {} already enrolled in section {}", studentId, sectionId);
            throw new RegistrationException("You are already registered for this section.");
        }
        log.debug("Duplicate enrollment check passed.");

        // 4. Fetch Target Section (used for both course check and capacity)
        Optional<Section> sectionOpt = sectionRepo.findById(sectionId);
        if (sectionOpt.isEmpty()) {
            throw new RegistrationException("The selected section does not exist.");
        }
        Section section = sectionOpt.get();
        int targetCourseId = section.courseId();

        // 5. Disallow multiple sections of the same course in the current term
        String currentSemester = settingsRepo.getCurrentSemester();
        int currentYear = settingsRepo.getCurrentYear();

        List<Enrollment> existingEnrollments = enrollmentRepo.findByStudentId(studentId);
        boolean alreadyRegisteredForCourse = existingEnrollments.stream().filter(e -> "Enrolled".equalsIgnoreCase(e.status())).map(e -> sectionRepo.findById(e.sectionId())).filter(Optional::isPresent).map(Optional::get).anyMatch(s ->
                        s.courseId() == targetCourseId && currentSemester.equalsIgnoreCase(s.semester()) && s.year() == currentYear);

        if (alreadyRegisteredForCourse) {
            log.warn("Registration blocked: student {} already enrolled in another section of course {} for {} {}", studentId, targetCourseId, currentSemester, currentYear);
            throw new RegistrationException(
                    "You are already registered in another section of this course for the current term.");
        }
        log.debug("Same-course check passed.");

        // 6. Check Section Capacity
        int currentEnrollment = sectionRepo.getEnrollmentCount(sectionId);
        if (currentEnrollment >= section.capacity()) {
            throw new RegistrationException("Registration failed: The section is full.");
        }
        log.debug("Capacity check passed.");

        // 7. Create Enrollment
        if (!enrollmentRepo.create(studentId, sectionId)) {
            throw new RegistrationException("An unexpected error occurred during registration.");
        }
        log.info("Registration successful.");
    }

    public List<RegistrationRow> getMyRegistrations(int studentId) {
        log.debug("Fetching registrations for student {}", studentId);
        List<RegistrationRow> registrationRows = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepo.findByStudentId(studentId);
        for (Enrollment enrollment : enrollments) {
            Optional<Section> sectionOpt = sectionRepo.findById(enrollment.sectionId());
            if (sectionOpt.isEmpty()) {
                log.warn("Skipping enrollment {}: Section {} not found.", enrollment.enrollmentId(), enrollment.sectionId());
                continue;
            }
            Section section = sectionOpt.get();
            Optional<Course> courseOpt = courseRepo.findById(section.courseId());
            if (courseOpt.isEmpty()) {
                log.warn("Skipping enrollment {}: Course {} not found.", enrollment.enrollmentId(), section.courseId());
                continue;
            }
            Course course = courseOpt.get();
            String instructorName = "Unassigned";
            if (section.instructorId() != null) {
                instructorName = instructorRepo.findProfileByUserId(section.instructorId())
                        .map(Instructor::name)
                        .orElse("Unknown Instructor");
            }
            RegistrationRow row = new RegistrationRow(
                    enrollment.enrollmentId(),
                    course.code(),
                    course.title(),
                    instructorName,
                    section.dayTime(),
                    section.room(),
                    enrollment.status()
            );
            registrationRows.add(row);
        }
        log.info("Found {} registrations for student {}", registrationRows.size(), studentId);
        return registrationRows;
    }

    public void dropSection(int studentId, int enrollmentId) throws DropException {
        log.info("Attempting to drop enrollment {} for student {}", enrollmentId, studentId);

        // 1. Check Maintenance Mode
        if (accessControl.isMaintenanceModeOn()) {
            log.warn("Drop blocked: Maintenance mode is ON.");
            throw new DropException("Cannot drop courses during system maintenance.");
        }
        log.debug("Maintenance mode check passed.");

        // 2. Check Drop Deadline
        Optional<LocalDate> deadlineOpt = settingsRepo.getDropDeadline();
        if (deadlineOpt.isEmpty()) {
            log.error("Drop blocked: Drop deadline is not configured in settings.");
            throw new DropException("The drop deadline has not been set by the administrator.");
        }
        LocalDate deadline = deadlineOpt.get();
        LocalDate today = LocalDate.now();
        log.debug("Comparing current date ({}) with deadline ({}) for enrollment {}", today, deadline, enrollmentId);
        if (today.isAfter(deadline)) {
            log.warn("Drop blocked: Deadline {} has passed for enrollment {}.", deadline, enrollmentId);
            throw new DropException("The deadline to drop this section (" + deadline + ") has passed.");
        }
        log.debug("Deadline check passed.");

        // 3. Verify Ownership
        log.debug("Verifying ownership for student {} and enrollment {}", studentId, enrollmentId);
        Optional<Enrollment> enrollOpt = enrollmentRepo.findById(enrollmentId);
        if (enrollOpt.isEmpty() || enrollOpt.get().studentId() != studentId) {
            log.error("Drop blocked: Enrollment {} not found or does not belong to student {}.", enrollmentId, studentId);
            throw new DropException("You are not enrolled in the section you are trying to drop, or the enrollment ID is incorrect.");
        }
        log.debug("Ownership check passed.");

        // 4. Delete Associated Grades first
        log.debug("Attempting to delete grades associated with enrollment {}", enrollmentId);
        boolean gradesDeleted = gradeRepo.deleteByEnrollmentId(enrollmentId); // Need to add this method to GradeRepository
        if (!gradesDeleted) {
            log.error("Drop failed: Could not delete associated grades for enrollment {}.", enrollmentId);
            throw new DropException("An error occurred while removing grade records. Drop cancelled.");
        }
        log.debug("Successfully deleted grades for enrollment {}", enrollmentId);

        // 5. Perform the enrollment drop (delete the record)
        log.debug("Attempting database delete for enrollment {}", enrollmentId);
        boolean enrollmentDeleted = enrollmentRepo.deleteById(enrollmentId);
        log.debug("Database delete result for enrollment: {}", enrollmentDeleted);
        if (!enrollmentDeleted) {
            log.error("Drop failed: Could not delete enrollment record {} after deleting grades.", enrollmentId);
            throw new DropException("An unexpected error occurred while dropping the section itself. Please contact support.");
        }

        log.info("Drop successful for enrollment {} by student {}", enrollmentId, studentId);
    }

    public List<GradeRow> getMyGrades(int studentId) {
        log.debug("Fetching grades for student {}", studentId);
        List<GradeRow> gradeRows = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepo.findByStudentId(studentId);
        if (enrollments.isEmpty()) {
            log.info("No enrollments found for student {}", studentId);
            return Collections.emptyList();
        }
        for (Enrollment enrollment : enrollments) {
            String courseCode = "N/A";
            String courseTitle = "Course Not Found";
            Optional<Section> sectionOpt = sectionRepo.findById(enrollment.sectionId());
            if (sectionOpt.isPresent()) {
                Optional<Course> courseOpt = courseRepo.findById(sectionOpt.get().courseId());
                if (courseOpt.isPresent()) {
                    courseCode = courseOpt.get().code();
                    courseTitle = courseOpt.get().title();
                } else {
                    log.warn("Could not find course details for section {}", enrollment.sectionId());
                }
            } else {
                log.warn("Could not find section details for enrollment {}", enrollment.enrollmentId());
            }
            List<Grade> grades = gradeRepo.findByEnrollmentId(enrollment.enrollmentId());
            if (grades.isEmpty()) {
                gradeRows.add(new GradeRow(courseCode, courseTitle, "No Grades Entered", "-", "-"));
            } else {
                for (Grade grade : grades) {
                    String scoreDisplay = (grade.score() != null) ? String.format("%.1f", grade.score()) : "Pending";
                    String componentDisplay = grade.component();
                    String finalGradeDisplay = grade.finalGrade() != null ? grade.finalGrade() : "-";
                    if ("Final Calculated Grade".equalsIgnoreCase(grade.component())) {
                        componentDisplay = "Overall Course Grade";
                        scoreDisplay = finalGradeDisplay;
                    }
                    GradeRow row = new GradeRow(courseCode, courseTitle, componentDisplay, scoreDisplay, finalGradeDisplay);
                    gradeRows.add(row);
                }
            }
        }
        log.info("Fetched {} grade components for student {}", gradeRows.size(), studentId);
        return gradeRows;
    }

    public List<TranscriptEntry> generateTranscriptData(int studentId) {
        log.debug("Generating transcript data for student {}", studentId);
        List<TranscriptEntry> transcriptEntries = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepo.findByStudentId(studentId);

        if (enrollments.isEmpty()) {
            log.info("No enrollments found for student {} for transcript.", studentId);
            return Collections.emptyList();
        }

        for (Enrollment enrollment : enrollments) {
            Optional<String> finalGradeOpt = gradeRepo.findByEnrollmentId(enrollment.enrollmentId()).stream().map(Grade::finalGrade).filter(fg -> fg != null && !fg.isBlank()).findFirst();

            // Only include courses where a final grade exists
            if (finalGradeOpt.isPresent()) {
                String finalGrade = finalGradeOpt.get();

                // Get Section and Course details
                Optional<Section> sectionOpt = sectionRepo.findById(enrollment.sectionId());
                if (sectionOpt.isPresent()) {
                    Section section = sectionOpt.get();
                    Optional<Course> courseOpt = courseRepo.findById(section.courseId());
                    if (courseOpt.isPresent()) {
                        Course course = courseOpt.get();
                        String term = section.semester() + " " + section.year();

                        TranscriptEntry entry = new TranscriptEntry(course.code(), course.title(), course.credits(), term, finalGrade);
                        transcriptEntries.add(entry);
                    } else {
                        log.warn("Transcript: Course {} not found for section {}", section.courseId(), section.sectionId());
                    }
                } else {
                    log.warn("Transcript: Section {} not found for enrollment {}", enrollment.sectionId(), enrollment.enrollmentId());
                }
            } else {
                log.debug("Transcript: No final grade found for enrollment {}. Skipping.", enrollment.enrollmentId());
            }
        }
        transcriptEntries.sort(Comparator.comparing(TranscriptEntry::semester).thenComparing(TranscriptEntry::courseCode));

        log.info("Generated {} transcript entries for student {}", transcriptEntries.size(), studentId);
        return transcriptEntries;
    }

    public List<TimetableEntry> getMyTimetable(int studentId) {
        log.debug("Fetching timetable for student {}", studentId);
        List<TimetableEntry> timetableEntries = new ArrayList<>();
        // Fetch only currently "Enrolled" sections
        List<Enrollment> enrollments = enrollmentRepo.findByStudentId(studentId).stream().filter(e -> "Enrolled".equalsIgnoreCase(e.status())).collect(Collectors.toList());

        if (enrollments.isEmpty()) {
            log.info("No active enrollments found for student {} timetable.", studentId);
            return Collections.emptyList();
        }

        for (Enrollment enrollment : enrollments) {
            Optional<Section> sectionOpt = sectionRepo.findById(enrollment.sectionId());
            if (sectionOpt.isEmpty()) {
                log.warn("Timetable: Section {} not found for enrollment {}.", enrollment.sectionId(), enrollment.enrollmentId());
                continue;
            }
            Section section = sectionOpt.get();

            Optional<Course> courseOpt = courseRepo.findById(section.courseId());
            if (courseOpt.isEmpty()) {
                log.warn("Timetable: Course {} not found for section {}.", section.courseId(), section.sectionId());
                continue;
            }
            Course course = courseOpt.get();

            String instructorName = "Unassigned";
            if (section.instructorId() != null) {
                instructorName = instructorRepo.findProfileByUserId(section.instructorId())
                        .map(Instructor::name)
                        .orElse("Unknown Instructor");
            }

            TimetableEntry entry = new TimetableEntry(course.code(), course.title(), section.dayTime() != null ? section.dayTime() : "N/A", section.room() != null ? section.room() : "N/A", instructorName);
            timetableEntries.add(entry);
        }

        timetableEntries.sort(Comparator.comparing(TimetableEntry::courseCode));

        log.info("Generated {} timetable entries for student {}", timetableEntries.size(), studentId);
        return timetableEntries;
    }
}