package com.meetbowl.application.minutes;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.minutes.Minutes;

/** 개인 워크스페이스 회의록 목록에 표시할 회의록 요약 결과다. */
public record MinutesListItemResult(
        UUID minutesId,
        UUID meetingId,
        UUID organizationId,
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

    public static MinutesListItemResult from(Minutes minutes, boolean favorite) {
        return from(minutes, favorite, MinutesMeetingMetadata.empty(minutes.reviewerUserId()));
    }

    public static MinutesListItemResult from(
            Minutes minutes, boolean favorite, MinutesMeetingMetadata metadata) {
        return new MinutesListItemResult(
                minutes.id(),
                minutes.meetingId(),
                minutes.organizationId(),
                minutes.reviewerUserId(),
                minutes.status().name(),
                minutes.summary(),
                minutes.approvedAt(),
                favorite,
                metadata.title(),
                metadata.startedAt(),
                metadata.endedAt(),
                metadata.attendeeCount(),
                metadata.reviewerName(),
                metadata.reviewerDepartment());
    }
}
