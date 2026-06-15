package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogSearchCommand(
        UUID actorUserId,
        String actorName,
        String actionType,
        String targetType,
        UUID targetId,
        String result,
        Instant from,
        Instant to,
        int page,
        int size) {}
