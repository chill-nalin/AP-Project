package edu.univ.erp.api.types;

public record ScoreEntryRequest(
        int enrollmentId,
        String component, // e.g., "Quiz", "Midterm", "EndSem"
        Double score
) {}