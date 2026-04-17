package com.rafptor.api.auth;

public enum UserRole {
    VIEWER,
    OPERATOR,
    REVIEWER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
