package edu.univ.erp.domain;

public record User(int userId, String username, Role role, Object profile) {

    public String getName() {

        return username;
    }
}

