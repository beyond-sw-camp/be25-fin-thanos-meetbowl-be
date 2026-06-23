package com.meetbowl.api.meeting.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.meeting.JoinMeetingResult;

/** 프론트가 room.connect에 바로 사용할 수 있는 회의 참여 응답이다. */
public record JoinMeetingResponse(
        UUID meetingId,
        String roomName,
        String livekitUrl,
        UUID hostUserId,
        String participantIdentity,
        String participantName,
        String token,
        Instant issuedAt,
        Instant expiresAt) {

    public static JoinMeetingResponse from(JoinMeetingResult result) {
        return new JoinMeetingResponse(
                result.meetingId(),
                result.roomName(),
                result.livekitUrl(),
                result.hostUserId(),
                result.participantIdentity(),
                result.participantName(),
                result.token(),
                result.issuedAt(),
                result.expiresAt());
    }
}
