package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.AdminAuditLogResult;

public record AdminAuditLogResponse(
        UUID auditLogId,
        UUID actorUserId,
        String actorName,
        String actionType,
        String actionLabel,
        String targetType,
        String targetTypeLabel,
        UUID targetId,
        String targetLoginId,
        String targetName,
        String result,
        String reason,
        String displayTitle,
        List<DisplayChangeItemResponse> displayChangeItems,
        Object beforeSnapshot,
        Object afterSnapshot,
        Instant createdAt) {

    public static AdminAuditLogResponse from(
            AdminAuditLogResult result, ObjectMapper objectMapper) {
        AdminAuditLogDisplayFormatter.DisplayInfo displayInfo =
                AdminAuditLogDisplayFormatter.format(result, objectMapper);
        return new AdminAuditLogResponse(
                result.auditLogId(),
                result.actorUserId(),
                result.actorName(),
                result.actionType(),
                displayInfo.actionLabel(),
                result.targetType(),
                displayInfo.targetTypeLabel(),
                result.targetId(),
                fallbackTargetValue(result.targetLoginId()),
                fallbackTargetValue(result.targetName()),
                result.result(),
                result.reason(),
                displayInfo.displayTitle(),
                displayInfo.displayChangeItems(),
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
            // 과거 로그가 JSON이 아닌 문자열로 저장돼 있어도 조회 API 자체는 계속 응답해야 한다.
            return snapshot;
        }
    }

    private static String fallbackTargetValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public record DisplayChangeItemResponse(
            String label, String beforeValue, String afterValue, String value) {}
}
