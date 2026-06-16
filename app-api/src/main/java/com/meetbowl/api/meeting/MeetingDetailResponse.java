package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.meeting.MeetingResult;

/**
 * 회의 상세 응답 DTO다. 목록 응답({@link MeetingResponse})과 달리 회의 내용({@code description})을 포함한다. 회의 본문은 상세
 * 조회(GET /meetings/{id})에서만 노출하고 목록에는 싣지 않는다.
 */
public record MeetingDetailResponse(
        UUID meetingId,
        String title,
        String description,
        Instant scheduledAt,
        Instant scheduledEndAt,
        UUID hostUserId,
        UUID meetingRoomId,
        String provider,
        String providerRoomId,
        String status,
        Instant startedAt,
        Instant endedAt,
        List<AttendeeResponse> attendees) {

    public static MeetingDetailResponse from(MeetingResult result) {
        return new MeetingDetailResponse(
                result.meetingId(),
                result.title(),
                result.description(),
                result.scheduledAt(),
                result.scheduledEndAt(),
                result.hostUserId(),
                result.meetingRoomId(),
                result.provider(),
                result.providerRoomId(),
                result.status(),
                result.startedAt(),
                result.endedAt(),
                result.attendees().stream().map(AttendeeResponse::from).toList());
    }
}
