package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

/**
 * 프론트가 room.connect(url, token)에 바로 사용할 수 있는 회의 접속 정보다.
 *
 * <p>브라우저는 더 이상 LiveKit JWT를 직접 만들지 않고, 이 결과만 사용해 회의에 입장한다.
 */
public record JoinMeetingResult(
        UUID meetingId,
        String roomName,
        String livekitUrl,
        UUID hostUserId,
        String participantIdentity,
        String participantName,
        String token,
        Instant issuedAt,
        Instant expiresAt) {}
