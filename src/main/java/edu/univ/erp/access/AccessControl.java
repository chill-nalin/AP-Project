package edu.univ.erp.access;

import edu.univ.erp.data.SectionRepository;
import edu.univ.erp.data.SettingsRepository;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class AccessControl {

    private static final Logger log = LoggerFactory.getLogger(AccessControl.class);
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final SectionRepository sectionRepo = new SectionRepository();

    public boolean isMaintenanceModeOn() {
        boolean isMaintenance = settingsRepo.isMaintenanceModeOn();
        if (isMaintenance) {
            log.debug("Access check: Maintenance mode is ON.");
        }
        return isMaintenance;
    }

    public boolean canInstructorGradeSection(User instructor, int sectionId) {
        if (instructor == null || instructor.role() != edu.univ.erp.domain.Role.Instructor) {
            log.warn("Access denied: User is not an instructor.");
            return false;
        }

        Optional<Section> sectionOpt = sectionRepo.findById(sectionId);
        if (sectionOpt.isEmpty()) {
            // Section doesn't exist
            log.warn("Access denied: Section {} not found for grading check.", sectionId);
            return false;
        }

        Section section = sectionOpt.get();
        boolean isAssigned = section.instructorId() != null && section.instructorId() == instructor.userId();

        if (!isAssigned) {
            log.warn("Access denied: Instructor {} attempted to grade section {} but is not assigned.",
                    instructor.userId(), sectionId);
        }
        return isAssigned;
    }
}