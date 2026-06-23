package com.meetbowl.domain.admin;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record AdminAuditLog(
        UUID id,
        UUID actorId,
        String actorName,
        String targetType,
        UUID targetId,
        String actionArea,
        String actionName,
        AuditResult result,
        String beforeValue,
        String afterValue,
        String ipAddress,
        String userAgent,
        Instant occurredAt) {

    public AdminAuditLog {
        if (actorId == null || occurredAt == null || result == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "감사 로그 필수 값이 누락되었습니다.");
        }
        validateText(actorName, "작업자명은 필수입니다.");
        validateText(targetType, "대상 타입은 필수입니다.");
        validateText(actionArea, "작업 영역은 필수입니다.");
        validateText(actionName, "작업명은 필수입니다.");
    }

    private static void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }
}
