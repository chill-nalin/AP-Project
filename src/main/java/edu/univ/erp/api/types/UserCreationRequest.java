package edu.univ.erp.api.types;

import edu.univ.erp.domain.Role;

public record UserCreationRequest(
        String username,
        String password,
        Role role,
        String name,        // For Instructor
        String rollNo,      // For Student
        String program,     // For Student
        int year,           // For Student
        String department   // For Instructor
) {}