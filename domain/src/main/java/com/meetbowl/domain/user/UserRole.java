package com.meetbowl.domain.user;

public enum UserRole {
    ADMIN,
    USER,
    GUEST,
    SYSTEM;

    public boolean canAccessAdminApi() {
        return this == ADMIN || this == SYSTEM;
    }
}
