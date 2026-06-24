package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.admin.AdminAuditLog;

public record AdminAuditLogResult(
        UUID auditLogId,
        UUID actorUserId,
        String actorName,
        String actionType,
        String targetType,
        UUID targetId,
        String targetLoginId,
        String targetName,
        String ipAddress,
        String result,
        String reason,
        String beforeSnapshot,
        String afterSnapshot,
        Instant createdAt) {

    static AdminAuditLogResult from(AdminAuditLog log) {
        return new AdminAuditLogResult(
                log.id(),
                log.actorId(),
                log.actorName(),
                toActionType(log.targetType(), log.actionName()),
                log.targetType(),
                log.targetId(),
                log.targetLoginId(),
                log.targetName(),
                log.ipAddress(),
                toApiResult(log.result().name()),
                "FAILURE".equals(log.result().name()) ? log.afterValue() : null,
                log.beforeValue(),
                log.afterValue(),
                log.occurredAt());
    }

    private static String toActionType(String targetType, String actionName) {
        if (actionName == null || actionName.isBlank()) {
            return actionName;
        }
        String prefix = targetType + "_";
        if (targetType != null && actionName.startsWith(prefix)) {
            return actionName;
        }
        return targetType == null || targetType.isBlank() ? actionName : prefix + actionName;
    }

    private static String toApiResult(String result) {
        return "FAILURE".equals(result) ? "FAILED" : result;
    }
}
