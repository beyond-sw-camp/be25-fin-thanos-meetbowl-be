package com.meetbowl.domain.mail;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 받은/보낸/휴지통 메일의 보관 기간과 자동 삭제 여부를 정의하는 조직 정책이다.
 *
 * <p>각 보관 기간은 1일 이상이어야 하며, 수정자/수정 시각을 필수로 남겨 누가 언제 정책을 바꿨는지 추적할 수 있게 한다.
 */
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
