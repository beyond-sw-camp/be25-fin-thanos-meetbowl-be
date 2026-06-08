package com.meetbowl.infrastructure.persistence.meeting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.meeting.MeetingProvider;
import com.meetbowl.domain.meeting.MeetingSession;
import com.meetbowl.domain.meeting.MeetingSessionStatus;
import com.meetbowl.domain.meeting.ParticipantSession;
import com.meetbowl.domain.meeting.ParticipantSessionStatus;
import com.meetbowl.domain.meeting.ParticipantType;

class MeetingEntityMappingTest {

    @Test
    void meetingSessionRoundTrip() {
        UUID id = UUID.randomUUID();
        MeetingSession source =
                MeetingSession.of(
                        id,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MeetingProvider.LIVEKIT,
                        "livekit-room",
                        MeetingSessionStatus.ACTIVE,
                        Instant.parse("2099-01-01T00:00:00Z"),
                        Instant.parse("2099-01-01T00:01:00Z"),
                        null);

        MeetingSession restored = MeetingSessionEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.meetingId()).isEqualTo(source.meetingId());
        assertThat(restored.providerRoomId()).isEqualTo(source.providerRoomId());
        assertThat(restored.status()).isEqualTo(MeetingSessionStatus.ACTIVE);
    }

    @Test
    void participantSessionRoundTrip() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ParticipantSession source =
                ParticipantSession.of(
                        id,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ParticipantType.PARTICIPANT,
                        userId,
                        null,
                        "회원",
                        "user-" + userId,
                        ParticipantSessionStatus.JOINED,
                        Instant.parse("2099-01-01T00:01:00Z"),
                        Instant.parse("2099-01-01T00:02:00Z"));

        ParticipantSession restored = ParticipantSessionEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.userId()).isEqualTo(userId);
        assertThat(restored.guestSessionId()).isNull();
        assertThat(restored.status()).isEqualTo(ParticipantSessionStatus.JOINED);
    }
}
