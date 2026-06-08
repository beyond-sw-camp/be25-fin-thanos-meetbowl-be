package com.meetbowl.domain.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ParticipantSessionTest {

    @Test
    void createMemberSession() {
        UUID userId = UUID.randomUUID();

        ParticipantSession session =
                ParticipantSession.createMember(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ParticipantType.PARTICIPANT,
                        userId,
                        "회원",
                        "user-" + userId);

        assertEquals(userId, session.userId());
        assertNull(session.guestSessionId());
        assertEquals(ParticipantSessionStatus.JOIN_REQUESTED, session.status());
    }

    @Test
    void createGuestSession() {
        UUID guestSessionId = UUID.randomUUID();

        ParticipantSession session =
                ParticipantSession.createGuest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        guestSessionId,
                        "게스트",
                        "guest-" + guestSessionId);

        assertEquals(ParticipantType.GUEST, session.participantType());
        assertNull(session.userId());
        assertEquals(guestSessionId, session.guestSessionId());
    }

    @Test
    void leftParticipantCanBeRestoredWithoutLeaveInfo() {
        ParticipantSession session =
                ParticipantSession.of(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ParticipantType.PARTICIPANT,
                        UUID.randomUUID(),
                        null,
                        "회원",
                        "user",
                        ParticipantSessionStatus.LEFT,
                        null,
                        null);

        assertEquals(ParticipantSessionStatus.LEFT, session.status());
    }
}
