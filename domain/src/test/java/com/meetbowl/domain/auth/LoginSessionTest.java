package com.meetbowl.domain.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LoginSessionTest {

    @Test
    void isUsable_success_when_active_and_not_expired() {
        Instant now = Instant.parse("2026-06-08T08:00:00Z");
        LoginSession session = session(true, now.plusSeconds(60));

        assertTrue(session.isUsable(now));
        assertFalse(session.isExpired(now));
    }

    @Test
    void isUsable_fail_when_expired() {
        Instant now = Instant.parse("2026-06-08T08:00:00Z");
        LoginSession session = session(true, now);

        assertTrue(session.isExpired(now));
        assertFalse(session.isUsable(now));
    }

    private static LoginSession session(boolean active, Instant expiresAt) {
        return new LoginSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "session-token-id",
                active,
                expiresAt,
                Instant.parse("2026-06-08T07:00:00Z"),
                "127.0.0.1",
                "JUnit",
                Instant.parse("2026-06-08T07:00:00Z"),
                Instant.parse("2026-06-08T07:00:00Z"));
    }
}
