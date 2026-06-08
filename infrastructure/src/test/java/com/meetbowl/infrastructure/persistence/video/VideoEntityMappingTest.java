package com.meetbowl.infrastructure.persistence.video;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.video.GuestSession;
import com.meetbowl.domain.video.GuestSessionStatus;
import com.meetbowl.domain.video.ParticipantSession;
import com.meetbowl.domain.video.ParticipantSessionStatus;
import com.meetbowl.domain.video.ParticipantType;
import com.meetbowl.domain.video.VideoProvider;
import com.meetbowl.domain.video.VideoRoom;
import com.meetbowl.domain.video.VideoRoomStatus;

class VideoEntityMappingTest {

    @Test
    void videoRoomRoundTrip() {
        UUID id = UUID.randomUUID();
        VideoRoom source =
                VideoRoom.of(
                        id,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        VideoProvider.LIVEKIT,
                        "livekit-room",
                        VideoRoomStatus.ACTIVE,
                        Instant.parse("2099-01-01T00:00:00Z"),
                        Instant.parse("2099-01-01T00:01:00Z"),
                        null,
                        null);

        VideoRoom restored = VideoRoomEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.meetingId()).isEqualTo(source.meetingId());
        assertThat(restored.providerRoomId()).isEqualTo(source.providerRoomId());
        assertThat(restored.status()).isEqualTo(VideoRoomStatus.ACTIVE);
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
                        Instant.parse("2099-01-01T00:02:00Z"),
                        null,
                        null);

        ParticipantSession restored = ParticipantSessionEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.userId()).isEqualTo(userId);
        assertThat(restored.guestSessionId()).isNull();
        assertThat(restored.status()).isEqualTo(ParticipantSessionStatus.JOINED);
    }

    @Test
    void guestSessionRoundTrip() {
        UUID id = UUID.randomUUID();
        GuestSession source =
                GuestSession.of(
                        id,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "게스트",
                        "hashed-token",
                        GuestSessionStatus.JOINED,
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T00:01:00Z"),
                        null,
                        "127.0.0.1",
                        "test-agent");

        GuestSession restored = GuestSessionEntity.from(source).toDomain();

        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.guestName()).isEqualTo("게스트");
        assertThat(restored.accessTokenHash()).isEqualTo("hashed-token");
        assertThat(restored.status()).isEqualTo(GuestSessionStatus.JOINED);
    }
}
