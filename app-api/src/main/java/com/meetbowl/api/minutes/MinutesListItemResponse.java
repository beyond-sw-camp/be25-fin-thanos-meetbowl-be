package com.meetbowl.api.minutes;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.minutes.MinutesListItemResult;

/** 개인 워크스페이스 회의록 목록 응답 DTO다. */
public record MinutesListItemResponse(
        UUID minutesId,
        UUID meetingId,
        UUID reviewerUserId,
        String status,
        String summary,
        Instant approvedAt,
        boolean favorite,
        String meetingTitle,
        Instant meetingStartedAt,
        Instant meetingEndedAt,
        int attendeeCount,
        String reviewerName,
        String reviewerDepartment) {

    public static MinutesListItemResponse from(MinutesListItemResult result) {
        return new MinutesListItemResponse(
                result.minutesId(),
                result.meetingId(),
                result.reviewerUserId(),
                result.status(),
                result.summary(),
                result.approvedAt(),
                result.favorite(),
                result.meetingTitle(),
                result.meetingStartedAt(),
                result.meetingEndedAt(),
                result.attendeeCount(),
                result.reviewerName(),
                result.reviewerDepartment());
    }
}
