package com.meetbowl.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class GuestSessionTest {

    @Test
    void createIssuedGuestSession() {
        UUID meetingId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2099-01-01T01:00:00Z");

        GuestSession guestSession =
                GuestSession.create(
                        meetingId,
                        UUID.randomUUID(),
                        "게스트",
                        "hashed-token",
                        expiresAt,
                        "127.0.0.1",
                        "test-agent");

        assertNull(guestSession.id());
        assertEquals(meetingId, guestSession.meetingId());
        assertEquals(GuestSessionStatus.ISSUED, guestSession.status());
        assertEquals(expiresAt, guestSession.expiresAt());
    }

    @Test
    void accessTokenHashMustNotBeBlank() {
        assertThrows(
                BusinessException.class,
                () ->
                        GuestSession.create(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "게스트",
                                " ",
                                Instant.parse("2099-01-01T01:00:00Z"),
                                null,
                                null));
    }
}
