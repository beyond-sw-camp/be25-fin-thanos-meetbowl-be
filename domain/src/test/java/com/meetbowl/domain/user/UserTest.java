package com.meetbowl.domain.user;

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
        User user = user(UserStatus.ACTIVE, NOW.minusSeconds(120), NOW.minusSeconds(60));

        assertTrue(user.isExpired(NOW));
        assertFalse(user.canLoginAt(NOW));
    }

    @Test
    void status_check_success_when_locked_or_inactive() {
        assertTrue(user(UserStatus.LOCKED, null, null).isLocked());
        assertTrue(user(UserStatus.INACTIVE, null, null).isInactive());
    }

    private static User user(UserStatus status, Instant activeFrom, Instant activeUntil) {
        return User.of(
                UUID.randomUUID(),
                "user01",
                "passwordHash",
                "홍길동",
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
}
