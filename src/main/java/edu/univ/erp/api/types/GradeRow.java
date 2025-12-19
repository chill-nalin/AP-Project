package edu.univ.erp.api.types;


public record GradeRow(
        String courseCode,
        String courseTitle,
        String component, // e.g., "Quiz", "Midterm", "Final Grade"
        String scoreDisplay, // Formatted score (e.g., "85.5", "90.0")
        String finalGrade // Final letter grade for the course
) {}