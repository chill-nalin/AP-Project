package edu.univ.erp.api.types;

// Record representing a single class session in the student's timetable.
public record TimetableEntry(
        String courseCode,
        String courseTitle,
        String dayTime, // e.g., "Mon/Wed 10:00-11:30"
        String room, // e.g., "C-201"
        String instructorName
) {}