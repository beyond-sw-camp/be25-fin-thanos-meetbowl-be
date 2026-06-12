package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingAttendee;

/**
 * 회의 출력 모델이다. app-api는 이 Result를 API Response DTO로 변환한다. 도메인 enum이 api 계층에 새어 나가지 않도록 {@code status}는
 * 문자열로 노출한다. {@code attendees}는 상세 조회 등에서 채워지며, 참석자가 필요 없는 경우 빈 목록이다. {@code description}(회의 내용)은
 * Result에는 항상 담기지만, 목록 응답 DTO는 이를 노출하지 않고 상세 응답 DTO에서만 내보낸다.
 */
public record MeetingResult(
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
        List<AttendeeResult> attendees) {

    public static MeetingResult of(Meeting meeting) {
        return of(meeting, List.of());
    }

    public static MeetingResult of(Meeting meeting, List<MeetingAttendee> attendees) {
        return new MeetingResult(
                meeting.id(),
                meeting.title(),
                meeting.description(),
                meeting.scheduledAt(),
                meeting.scheduledEndAt(),
                meeting.hostUserId(),
                meeting.meetingRoomId(),
                meeting.provider(),
                meeting.providerRoomId(),
                meeting.status() == null ? null : meeting.status().name(),
                meeting.startedAt(),
                meeting.endedAt(),
                attendees.stream().map(AttendeeResult::of).toList());
    }
}