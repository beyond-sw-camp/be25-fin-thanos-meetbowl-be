package com.meetbowl.domain.mail;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record MailRetentionPolicy(
        UUID id,
        int inboxRetentionDays,
        int sentRetentionDays,
        int trashRetentionDays,
        boolean autoDeleteEnabled,
        UUID updatedBy,
        Instant updatedAt) {

    public MailRetentionPolicy {
        validatePositive(inboxRetentionDays, "받은 메일함 보관 기간은 1일 이상이어야 합니다.");
        validatePositive(sentRetentionDays, "보낸 메일함 보관 기간은 1일 이상이어야 합니다.");
        validatePositive(trashRetentionDays, "휴지통 보관 기간은 1일 이상이어야 합니다.");
        if (updatedBy == null || updatedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "메일 보관 정책 수정 정보는 필수입니다.");
        }
    }

    private static void validatePositive(int value, String message) {
        if (value < 1) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }
}
