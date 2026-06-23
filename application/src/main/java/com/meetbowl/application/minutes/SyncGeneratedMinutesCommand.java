package com.meetbowl.application.minutes;

import java.util.UUID;

/** AI 서버가 생성한 회의록 초안을 저장하기 위한 application 입력이다. */
public record SyncGeneratedMinutesCommand(
        UUID eventId,
        UUID meetingId,
        UUID organizationId,
        UUID reviewerUserId,
        String summary,
        String content,
        String model,
        String promptVersion) {}
