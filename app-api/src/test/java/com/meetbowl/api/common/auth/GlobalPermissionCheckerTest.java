package com.meetbowl.api.common.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class GlobalPermissionCheckerTest {

    private final GlobalPermissionChecker permissionChecker = new GlobalPermissionChecker();

    @Test
    void requireAdminAllowsAdmin() {
        AuthenticatedUser admin = user(AuthenticatedUserRole.ADMIN);

        assertDoesNotThrow(() -> permissionChecker.requireAdmin(admin));
    }

    @Test
    void requireAdminRejectsUser() {
        AuthenticatedUser user = user(AuthenticatedUserRole.USER);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> permissionChecker.requireAdmin(user));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void requireUserOrAdminRejectsSystem() {
        AuthenticatedUser guest = user(AuthenticatedUserRole.SYSTEM);

        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> permissionChecker.requireUserOrAdmin(guest));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void nullUserReturnsUnauthorized() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> permissionChecker.requireUserOrAdmin(null));

        assertEquals(ErrorCode.COMMON_UNAUTHORIZED, exception.errorCode());
    }

    private AuthenticatedUser user(AuthenticatedUserRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), role, "홍길동");
    }
}
