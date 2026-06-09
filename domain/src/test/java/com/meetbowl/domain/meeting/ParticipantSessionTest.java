package com.meetbowl.domain.meeting;

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
                        ParticipantType.PARTICIPANT,
                        userId,
                        "회원",
                        "user-" + userId);

        assertEquals(userId, session.userId());
        assertEquals(ParticipantSessionStatus.JOIN_REQUESTED, session.status());
    }

    @Test
    void createGuestSession() {
        UUID meetingId = UUID.randomUUID();

        ParticipantSession session =
                ParticipantSession.createGuest(meetingId, "게스트", "guest-" + UUID.randomUUID());

        assertEquals(meetingId, session.meetingId());
        assertEquals(ParticipantType.GUEST, session.participantType());
        assertNull(session.userId());
    }

    @Test
    void leftParticipantCanBeRestoredWithoutLeaveInfo() {
        ParticipantSession session =
                ParticipantSession.of(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ParticipantType.PARTICIPANT,
                        UUID.randomUUID(),
                        "회원",
                        "user",
                        ParticipantSessionStatus.LEFT,
                        null,
                        null);

        assertEquals(ParticipantSessionStatus.LEFT, session.status());
    }

    @Test
    void guestCannotHaveMemberUserId() {
        assertThrows(
                BusinessException.class,
                () ->
                        ParticipantSession.of(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                ParticipantType.GUEST,
                                UUID.randomUUID(),
                                "게스트",
                                "guest",
                                ParticipantSessionStatus.JOIN_REQUESTED,
                                null,
                                null));
    }
}
