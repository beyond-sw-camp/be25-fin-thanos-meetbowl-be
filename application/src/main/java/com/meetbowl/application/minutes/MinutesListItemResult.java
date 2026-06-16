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
        String title,
        String status,
        String summary,
        Instant approvedAt,
        boolean favorite) {

    private static final String DEFAULT_TITLE = "회의록";

    public static MinutesListItemResult from(Minutes minutes, boolean favorite) {
        return new MinutesListItemResult(
                minutes.id(),
                minutes.meetingId(),
                minutes.organizationId(),
                minutes.reviewerUserId(),
                DEFAULT_TITLE,
                minutes.status().name(),
                minutes.summary(),
                minutes.approvedAt(),
                favorite);
    }
}
