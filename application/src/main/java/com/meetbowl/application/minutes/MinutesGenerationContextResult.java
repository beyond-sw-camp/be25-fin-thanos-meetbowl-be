package com.meetbowl.application.minutes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** AI 회의록 생성에 필요한 회의 메타데이터와 Final Transcript를 묶은 내부 조회 결과다. */
public record MinutesGenerationContextResult(
        UUID meetingId,
        UUID organizationId,
        UUID hostUserId,
        UUID reviewerUserId,
        String title,
        Instant startedAt,
        Instant endedAt,
        List<Participant> participants,
        String rawTranscript) {

    public record Participant(UUID userId, String name, String department) {}
}
