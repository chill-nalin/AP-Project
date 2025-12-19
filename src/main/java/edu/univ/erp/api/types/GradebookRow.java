package edu.univ.erp.api.types;

// Record representing a single row in the Instructor's Gradebook view.
public record GradebookRow(
        int enrollmentId,
        int studentId,
        String studentRollNo,
        Double quizScore,
        Double midtermScore,
        Double endSemScore,
        String finalGrade
) {}