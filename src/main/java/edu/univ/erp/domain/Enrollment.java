package edu.univ.erp.domain;

public record Enrollment(
        int enrollmentId,
        int studentId,
        int sectionId,
        String status // e.g., "Enrolled", "Dropped"
) {}