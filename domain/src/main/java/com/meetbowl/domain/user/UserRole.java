package com.meetbowl.domain.user;

public enum UserRole {
    ADMIN,
    USER,
    SYSTEM;

    public boolean canAccessAdminApi() {
        return this == ADMIN;
    }
}
