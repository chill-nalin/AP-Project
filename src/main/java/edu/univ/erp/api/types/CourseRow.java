package edu.univ.erp.api.types;

public record CourseRow(
        int sectionId,
        String courseCode,
        String title,
        int credits,
        String instructorName,
        String dayTime,
        String room,
        int capacity,
        int enrolledCount
) {

    public String getAvailability() {
        return enrolledCount + " / " + capacity;
    }

    public boolean isFull() {
        return enrolledCount >= capacity;
    }
}