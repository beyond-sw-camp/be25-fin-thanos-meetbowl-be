package com.meetbowl.application.minutes;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.minutes.Minutes;

/** 회의록 변경 UseCase 결과다. API 계층은 이 값을 외부 응답 DTO로 변환한다. */
public record MinutesResult(
        UUID minutesId,
        UUID meetingId,
        UUID organizationId,
        UUID reviewerUserId,
        String status,
        String summary,
        String content,
        Instant approvedAt) {

    /** API 계층이 domain enum에 직접 의존하지 않도록 상태를 외부 계약 문자열로 변환한다. */
    public static MinutesResult from(Minutes minutes) {
        return new MinutesResult(
                minutes.id(),
                minutes.meetingId(),
                minutes.organizationId(),
                minutes.reviewerUserId(),
                minutes.status().name(),
                minutes.summary(),
                minutes.content(),
                minutes.approvedAt());
    }
}
