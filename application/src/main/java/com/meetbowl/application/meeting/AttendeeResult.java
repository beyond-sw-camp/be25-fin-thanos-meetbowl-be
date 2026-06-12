package com.meetbowl.application.meeting;

import java.util.UUID;

import com.meetbowl.domain.meeting.MeetingAttendee;

/** 회의 참석자 출력 모델이다. 사용자 이름 등 상세는 app-api/FE가 사용자 도메인으로 별도 조회한다. */
public record AttendeeResult(UUID userId, String role, String attendanceStatus) {

    public static AttendeeResult of(MeetingAttendee attendee) {
        return new AttendeeResult(
                attendee.userId(),
                attendee.role().name(),
                attendee.attendanceStatus().name());
    }
}
