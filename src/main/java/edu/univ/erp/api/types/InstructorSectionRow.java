package edu.univ.erp.api.types;

public record InstructorSectionRow(
        int sectionId,
        String courseCode,
        String courseTitle,
        String dayTime,
        String room,
        int enrollmentCount,
        int capacity
) {
    public String getLoad() {
        return enrollmentCount + " / " + capacity;
    }
}