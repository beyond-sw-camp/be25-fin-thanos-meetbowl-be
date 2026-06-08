package com.meetbowl.domain.minutes;

/** docs/api-spec.md의 회의록 상태 값을 도메인에서 사용하는 enum이다. */
public enum MinutesStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    SHARED,
    DELETION_SCHEDULED
}
