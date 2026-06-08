package com.meetbowl.domain.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class VideoRoomTest {

    @Test
    void createReadyVideoRoom() {
        UUID meetingId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID hostUserId = UUID.randomUUID();

        VideoRoom videoRoom =
                VideoRoom.create(
                        meetingId,
                        organizationId,
                        hostUserId,
                        VideoProvider.LIVEKIT,
                        "meeting-" + meetingId);

        assertNull(videoRoom.id());
        assertEquals(meetingId, videoRoom.meetingId());
        assertEquals(organizationId, videoRoom.organizationId());
        assertEquals(hostUserId, videoRoom.hostUserId());
        assertEquals(VideoProvider.LIVEKIT, videoRoom.provider());
        assertEquals(VideoRoomStatus.READY, videoRoom.status());
    }

    @Test
    void endedRoomRequiresEndedAtAndReason() {
        assertThrows(
                BusinessException.class,
                () ->
                        VideoRoom.of(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                VideoProvider.LIVEKIT,
                                "room",
                                VideoRoomStatus.ENDED,
                                Instant.parse("2099-01-01T00:00:00Z"),
                                Instant.parse("2099-01-01T00:01:00Z"),
                                null,
                                null));
    }
}
