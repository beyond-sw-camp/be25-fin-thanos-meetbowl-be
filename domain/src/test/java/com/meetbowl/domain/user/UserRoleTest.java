package com.meetbowl.domain.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void canAccessAdminApi_success_only_admin() {
        assertTrue(UserRole.ADMIN.canAccessAdminApi());
        assertFalse(UserRole.SYSTEM.canAccessAdminApi());
        assertFalse(UserRole.USER.canAccessAdminApi());
    }
}
