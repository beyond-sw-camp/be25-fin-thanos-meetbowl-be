package com.meetbowl.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

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
    void guestParticipantCannotHaveUserId() {
        assertThrows(
                BusinessException.class,
                () ->
                        ParticipantSession.of(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                ParticipantType.GUEST,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "게스트",
                                "guest",
                                ParticipantSessionStatus.JOIN_REQUESTED,
                                null,
                                null,
                                null,
                                null));
    }
}
