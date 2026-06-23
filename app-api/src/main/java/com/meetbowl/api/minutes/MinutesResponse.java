package com.meetbowl.api.minutes;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.minutes.MinutesResult;

/** 회의록 수정 및 승인 결과를 외부 API 계약으로 변환한다. */
public record MinutesResponse(
        UUID minutesId,
        UUID meetingId,
        UUID reviewerUserId,
        String status,
        String summary,
        String content,
        Instant approvedAt,
        String meetingTitle,
        Instant meetingStartedAt,
        Instant meetingEndedAt,
        int attendeeCount,
        String reviewerName,
        String reviewerDepartment) {

    public static MinutesResponse from(MinutesResult result) {
        return new MinutesResponse(
                result.minutesId(),
                result.meetingId(),
                result.reviewerUserId(),
                result.status(),
                result.summary(),
                result.content(),
                result.approvedAt(),
                result.meetingTitle(),
                result.meetingStartedAt(),
                result.meetingEndedAt(),
                result.attendeeCount(),
                result.reviewerName(),
                result.reviewerDepartment());
    }
}
