package edu.univ.erp.service;

import edu.univ.erp.api.types.CourseRow;
import edu.univ.erp.data.CourseRepository;
import edu.univ.erp.data.InstructorProfileRepository;
import edu.univ.erp.data.SectionRepository;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final SectionRepository sectionRepo = new SectionRepository();
    private final CourseRepository courseRepo = new CourseRepository();
    private final InstructorProfileRepository instructorRepo = new InstructorProfileRepository();

    public List<CourseRow> getCatalog(String semester, int year) {
        log.debug("Fetching catalog for {}-{}", semester, year);
        List<CourseRow> catalog = new ArrayList<>();

        // 1. Get all sections for the term
        List<Section> sections = sectionRepo.findAllBySemesterAndYear(semester, year);

        // 2. For each section, enrich it with data from other tables
        for (Section section : sections) {
            Optional<Course> courseOpt = courseRepo.findById(section.courseId());
            if (courseOpt.isEmpty()) {
                log.warn("Skipping section {}: No matching course found for courseId {}",
                        section.sectionId(), section.courseId());
                continue;
            }
            Course course = courseOpt.get();

            // Get Instructor details
            String instructorName = "Unassigned";
            if (section.instructorId() != null) {
                Optional<Instructor> instructorOpt = instructorRepo.findProfileByUserId(section.instructorId());
                instructorName = instructorOpt.map(Instructor::name).orElse("Unknown Instructor");
            }

            // Get current enrollment count
            int enrolledCount = sectionRepo.getEnrollmentCount(section.sectionId());

            // 4. Assemble the final CourseRow
            CourseRow row = new CourseRow(section.sectionId(), course.code(), course.title(), course.credits(), instructorName, section.dayTime(), section.room(), section.capacity(), enrolledCount);
            catalog.add(row);
        }

        log.info("Catalog fetch complete, found {} sections.", catalog.size());
        return catalog;
    }
}