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
        Instant approvedAt) {

    public static MinutesResponse from(MinutesResult result) {
        return new MinutesResponse(
                result.minutesId(),
                result.meetingId(),
                result.reviewerUserId(),
                result.status(),
                result.summary(),
                result.approvedAt());
    }
}
