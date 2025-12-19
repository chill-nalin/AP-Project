package edu.univ.erp.api.types;

public record TranscriptEntry(
        String courseCode,
        String courseTitle,
        int credits,
        String semester, // e.g., "Fall 2025"
        String finalGrade // e.g., "A+", "B"
) {}