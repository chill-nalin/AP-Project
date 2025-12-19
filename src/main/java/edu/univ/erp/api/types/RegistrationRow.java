package edu.univ.erp.api.types;

// Record representing a single row in the "My Registrations" view.
public record RegistrationRow(
        int enrollmentId,
        String courseCode,
        String title,
        String instructorName,
        String dayTime,
        String room,
        String status // e.g., "Enrolled"
) {}