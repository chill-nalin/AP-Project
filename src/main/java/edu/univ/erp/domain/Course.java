package edu.univ.erp.domain;

public record Course(
        int courseId,
        String code,
        String title,
        int credits
) {}