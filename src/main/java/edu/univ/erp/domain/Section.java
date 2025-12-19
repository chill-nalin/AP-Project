package edu.univ.erp.domain;

public record Section(
        int sectionId,
        int courseId,
        Integer instructorId,
        String dayTime,
        String room,
        int capacity,
        String semester,
        int year
) {}