package com.meetbowl.api.meeting;

import java.util.UUID;

import com.meetbowl.application.meeting.AttendeeResult;

/** 회의 참석자 응답 DTO다. 사용자 이름 등 상세는 FE가 사용자 API로 별도 조회한다. */
public record AttendeeResponse(UUID userId, String role, String attendanceStatus) {

    public static AttendeeResponse from(AttendeeResult result) {
        return new AttendeeResponse(result.userId(), result.role(), result.attendanceStatus());
    }
}