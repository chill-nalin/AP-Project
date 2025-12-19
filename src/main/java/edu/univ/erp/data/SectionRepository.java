package edu.univ.erp.data;

import edu.univ.erp.domain.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SectionRepository {

    private static final Logger log = LoggerFactory.getLogger(SectionRepository.class);
    private final DbManager dbManager = DbManager.getInstance();

    public List<Section> findAllBySemesterAndYear(String semester, int year) {
        List<Section> sections = new ArrayList<>();
        String sql = "SELECT section_id, course_id, instructor_id, day_time, room, capacity " +
                "FROM sections WHERE semester = ? AND year = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, semester);
            pstmt.setInt(2, year);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Section section = new Section(
                            rs.getInt("section_id"), rs.getInt("course_id"),
                            (Integer) rs.getObject("instructor_id"), rs.getString("day_time"),
                            rs.getString("room"), rs.getInt("capacity"),
                            semester, year
                    );
                    sections.add(section);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error finding all sections", e);
        }
        return sections;
    }

    public int getEnrollmentCount(int sectionId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE section_id = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error getting enrollment count for section: {}", sectionId, e);
        }
        return 0;
    }

    public Optional<Section> findById(int sectionId) {
        String sql = "SELECT section_id, course_id, instructor_id, day_time, room, " +
                "capacity, semester, year FROM sections WHERE section_id = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sectionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Section section = new Section(
                            rs.getInt("section_id"), rs.getInt("course_id"),
                            (Integer) rs.getObject("instructor_id"), rs.getString("day_time"),
                            rs.getString("room"), rs.getInt("capacity"),
                            rs.getString("semester"), rs.getInt("year")
                    );
                    return Optional.of(section);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error finding section by id: {}", sectionId, e);
        }
        return Optional.empty();
    }

    public List<Section> findByInstructorIdAndTerm(int instructorId, String semester, int year) {
        List<Section> sections = new ArrayList<>();
        String sql = "SELECT section_id, course_id, instructor_id, day_time, room, capacity " +
                "FROM sections WHERE instructor_id = ? AND semester = ? AND year = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructorId);
            pstmt.setString(2, semester);
            pstmt.setInt(3, year);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Section section = new Section(
                            rs.getInt("section_id"), rs.getInt("course_id"),
                            instructorId, // We know this
                            rs.getString("day_time"), rs.getString("room"),
                            rs.getInt("capacity"), semester, year
                    );
                    sections.add(section);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error finding sections for instructor {}", instructorId, e);
        }
        return sections;
    }

    public boolean createSection(int courseId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) {
        String sql = "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. course_id
            pstmt.setInt(1, courseId);

            // 2. instructor_id (Conditional binding)
            if (instructorId != null) {
                pstmt.setInt(2, instructorId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }

            pstmt.setString(3, dayTime);

            pstmt.setString(4, room);

            pstmt.setInt(5, capacity);

            pstmt.setString(6, semester);

            pstmt.setInt(7, year);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                log.info("Created section for courseId {} in {}-{}", courseId, semester, year);
                return true;
            } else {
                log.warn("Section creation failed unexpectedly (0 rows affected) for courseId {}", courseId);
                return false;
            }
        } catch (SQLException e) {
            log.error("SQL error creating section for courseId {}. Details: {}", courseId, e.getMessage(), e);
            return false;
        }
    }

    public boolean updateInstructor(int sectionId, Integer instructorId) {
        String sql = "UPDATE sections SET instructor_id = ? WHERE section_id = ?";
        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (instructorId != null) {
                pstmt.setInt(1, instructorId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, sectionId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Updated instructor for sectionId {} to instructorId {}", sectionId, instructorId);
                return true;
            } else {
                log.warn("Failed to update instructor for sectionId {}: Section not found?", sectionId);
                return false;
            }
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                log.warn("Failed to assign instructor {} to section {}: Instructor does not exist.", instructorId, sectionId);
            } else {
                log.error("SQL error updating instructor for sectionId {}", sectionId, e);
            }
            return false;
        }
    }

    public boolean deleteById(int sectionId) {
        String sql = "DELETE FROM sections WHERE section_id = ?";
        log.debug("Attempting to delete section with ID {}", sectionId);

        try (Connection conn = dbManager.getErpConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sectionId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                log.info("Successfully deleted section {}", sectionId);
                return true;
            } else {
                log.warn("Section deletion failed for ID {}: Section not found?", sectionId);
                return false;
            }
        } catch (SQLException e) {
            if (e.getSQLState().startsWith("23")) {
                log.error("Cannot delete section {} because other records reference it.", sectionId, e);
            } else {
                log.error("SQL error deleting section {}", sectionId, e);
            }
            return false;
        }
    }

    public boolean updateSection(int sectionId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) {

        String sql = "UPDATE sections SET instructor_id = ?, day_time = ?, room = ?, " + "capacity = ?, semester = ?, year = ? WHERE section_id = ?";

        try (Connection con = dbManager.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (instructorId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, instructorId);
            }

            ps.setString(2, dayTime);
            ps.setString(3, room);
            ps.setInt(4, capacity);
            ps.setString(5, semester);
            ps.setInt(6, year);
            ps.setInt(7, sectionId);

            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update section with ID " + sectionId, e);
        }
    }

    public List<Section> findSectionsByCourse(int courseId) {
        String sql = "SELECT section_id, course_id, instructor_id, day_time, room, capacity, semester, year " +
                "FROM sections WHERE course_id = ?";
        List<Section> list = new ArrayList<>();

        try (Connection con = dbManager.getErpConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Section(
                            rs.getInt("section_id"),
                            rs.getInt("course_id"),
                            (Integer) rs.getObject("instructor_id"),
                            rs.getString("day_time"),
                            rs.getString("room"),
                            rs.getInt("capacity"),
                            rs.getString("semester"),
                            rs.getInt("year")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load sections for courseId " + courseId, e);
        }

        return list;
    }
}