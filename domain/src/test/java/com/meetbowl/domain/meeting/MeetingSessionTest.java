package com.meetbowl.domain.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MeetingSessionTest {

    @Test
    void createReadyMeetingSession() {
        UUID meetingId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();

        MeetingSession meetingSession =
                MeetingSession.create(
                        meetingId,
                        organizationId,
                        hostUserId,
                        MeetingProvider.LIVEKIT,
                        "meeting-" + meetingId);

        assertNull(meetingSession.id());
        assertEquals(meetingId, meetingSession.meetingId());
        assertEquals(organizationId, meetingSession.organizationId());
        assertEquals(hostUserId, meetingSession.hostUserId());
        assertEquals(MeetingProvider.LIVEKIT, meetingSession.provider());
        assertEquals(MeetingSessionStatus.READY, meetingSession.status());
    }

    @Test
    void endedRoomCanBeRestoredWithEndedAtOnly() {
        MeetingSession meetingSession =
                MeetingSession.of(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MeetingProvider.LIVEKIT,
                        "room",
                        MeetingSessionStatus.ENDED,
                        null,
                        null,
                        Instant.parse("2099-01-01T00:01:00Z"));

        assertEquals(MeetingSessionStatus.ENDED, meetingSession.status());
        assertEquals(Instant.parse("2099-01-01T00:01:00Z"), meetingSession.endedAt());
    }
}
