package com.meetbowl.application.minutes;

import java.time.Instant;
import java.util.UUID;

/** 회의록 화면 표시를 위해 회의·참석자·검토자 정보를 조립한 메타데이터다. */
public record MinutesMeetingMetadata(
        String title,
        Instant startedAt,
        Instant endedAt,
        int attendeeCount,
        UUID reviewerUserId,
        String reviewerName,
        String reviewerDepartment) {

    public static MinutesMeetingMetadata empty(UUID reviewerUserId) {
        return new MinutesMeetingMetadata(null, null, null, 0, reviewerUserId, null, null);
    }
}
