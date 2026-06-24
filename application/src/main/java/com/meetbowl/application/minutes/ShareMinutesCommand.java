package com.meetbowl.application.minutes;

import java.util.List;
import java.util.UUID;

/** 승인된 회의록을 회의 미참석자에게 수동 공유하는 입력이다. */
public record ShareMinutesCommand(
        UUID meetingId,
        UUID actorUserId,
        UUID actorOrganizationId,
        List<UUID> recipientUserIds,
        String subject,
        String body,
        UUID idempotencyKey) {}
