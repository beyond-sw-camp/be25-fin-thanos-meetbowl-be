package com.meetbowl.domain.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogSearchCondition(
        UUID actorUserId,
        String actorName,
        String actionType,
        String targetType,
        UUID targetId,
        AuditResult result,
        Instant from,
        Instant to,
        int page,
        int size) {}
