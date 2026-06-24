package com.meetbowl.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-06-08T08:00:00Z");

    @Test
    void canLoginAt_success_when_active_user_in_active_period() {
        User user = user(UserStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(60));

        assertTrue(user.canLoginAt(NOW));
        assertFalse(user.isInactive());
        assertFalse(user.isLocked());
    }

    @Test
    void isExpired_success_when_active_until_passed() {
        User user =
                user(
                        UserStatus.ACTIVE,
                        Instant.parse("2026-06-06T00:00:00Z"),
                        Instant.parse("2026-06-07T00:00:00Z"));

        assertTrue(user.isExpired(NOW));
        assertFalse(user.canLoginAt(NOW));
    }

    @Test
    void status_check_success_when_locked_or_inactive() {
        assertTrue(user(UserStatus.LOCKED, null, null).isLocked());
        assertTrue(user(UserStatus.INACTIVE, null, null).isInactive());
    }

    @Test
    void effectiveStatus_usesInclusiveDateBoundaries() {
        Instant today = Instant.parse("2026-06-23T12:00:00Z");

        User expired =
                user(
                        UserStatus.ACTIVE,
                        Instant.parse("2026-06-19T00:00:00Z"),
                        Instant.parse("2026-06-21T00:00:00Z"));
        User activeToday =
                user(
                        UserStatus.ACTIVE,
                        Instant.parse("2026-06-19T00:00:00Z"),
                        Instant.parse("2026-06-23T00:00:00Z"));
        User future =
                user(
                        UserStatus.ACTIVE,
                        Instant.parse("2026-06-24T00:00:00Z"),
                        null);

        assertEquals(UserStatus.INACTIVE, expired.effectiveStatusAt(today));
        assertEquals(UserStatus.ACTIVE, activeToday.effectiveStatusAt(today));
        assertEquals(UserStatus.INACTIVE, future.effectiveStatusAt(today));
    }

    @Test
    void completeInitialPasswordChange_updatesHashAndClearsRequiredFlag() {
        User user = initialPasswordUser();

        User changed = user.completeInitialPasswordChange("newPasswordHash");

        assertEquals("newPasswordHash", changed.passwordHash());
        assertFalse(changed.initialPasswordChangeRequired());
    }

    @Test
    void resetPasswordByAdmin_updatesHashAndRequiresInitialPasswordChange() {
        User user = regularUser();

        User changed = user.resetPasswordByAdmin("adminResetPasswordHash");

        assertEquals("adminResetPasswordHash", changed.passwordHash());
        assertTrue(changed.initialPasswordChangeRequired());
    }

    @Test
    void changePassword_updatesHashAndClearsRequiredFlag() {
        User user = initialPasswordUser();

        User changed = user.changePassword("changedPasswordHash");

        assertEquals("changedPasswordHash", changed.passwordHash());
        assertFalse(changed.initialPasswordChangeRequired());
    }

    private static User user(UserStatus status, Instant activeFrom, Instant activeUntil) {
        return User.of(
                UUID.randomUUID(),
                "user01",
                "passwordHash",
                "Test User",
                "user01@example.com",
                UserRole.USER,
                status,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                activeFrom,
                activeUntil,
                NOW,
                NOW);
    }

    private static User initialPasswordUser() {
        return User.of(
                UUID.randomUUID(),
                "user01",
                "passwordHash",
                "Test User",
                "user01@example.com",
                UserRole.USER,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                NOW,
                NOW);
    }

    private static User regularUser() {
        return User.of(
                UUID.randomUUID(),
                "user01",
                "passwordHash",
                "Test User",
                "user01@example.com",
                UserRole.USER,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                NOW,
                NOW);
    }
}
