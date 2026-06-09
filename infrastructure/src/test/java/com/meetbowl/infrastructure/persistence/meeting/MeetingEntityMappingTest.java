package com.meetbowl.infrastructure.persistence.meeting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.meeting.ParticipantSession;
import com.meetbowl.domain.meeting.ParticipantSessionStatus;
import com.meetbowl.domain.meeting.ParticipantType;

class MeetingEntityMappingTest {

    @Test
    void participantSessionRoundTrip() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ParticipantSession source =
                ParticipantSession.of(
                        id,
                        UUID.randomUUID(),
                        ParticipantType.PARTICIPANT,
                        userId,
                        "회원",
                        "user-" + userId,
                        ParticipantSessionStatus.JOINED,
                        Instant.parse("2099-01-01T00:01:00Z"),
                        Instant.parse("2099-01-01T00:02:00Z"));

        ParticipantSession restored = ParticipantSessionEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.userId()).isEqualTo(userId);
        assertThat(restored.status()).isEqualTo(ParticipantSessionStatus.JOINED);
    }
}
