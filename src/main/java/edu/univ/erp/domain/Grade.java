package edu.univ.erp.domain;

public record Grade(
        int gradeId,
        int enrollmentId,
        String component,
        Double score,
        String finalGrade // "A+", "B", etc.
) {}