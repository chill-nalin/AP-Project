package edu.univ.erp.auth;

import edu.univ.erp.domain.Role;

public record UserAuthData(
        int userId,
        Role role,
        String passwordHash
) {}