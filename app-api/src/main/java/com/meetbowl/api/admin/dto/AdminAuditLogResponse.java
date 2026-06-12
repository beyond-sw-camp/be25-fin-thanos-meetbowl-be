package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.AdminAuditLogResult;

public record AdminAuditLogResponse(
        UUID auditLogId,
        UUID actorUserId,
        String actorName,
        String actionType,
        String targetType,
        UUID targetId,
        String result,
        String reason,
        Object beforeSnapshot,
        Object afterSnapshot,
        Instant createdAt) {

    public static AdminAuditLogResponse from(
            AdminAuditLogResult result, ObjectMapper objectMapper) {
        return new AdminAuditLogResponse(
                result.auditLogId(),
                result.actorUserId(),
                result.actorName(),
                result.actionType(),
                result.targetType(),
                result.targetId(),
                result.result(),
                result.reason(),
                toSnapshotValue(result.beforeSnapshot(), objectMapper),
                toSnapshotValue(result.afterSnapshot(), objectMapper),
                result.createdAt());
    }

    private static Object toSnapshotValue(String snapshot, ObjectMapper objectMapper) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshot, Object.class);
        } catch (Exception exception) {
            return snapshot;
        }
    }
}
