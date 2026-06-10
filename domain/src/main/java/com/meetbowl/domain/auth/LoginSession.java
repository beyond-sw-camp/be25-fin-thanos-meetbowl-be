package com.meetbowl.domain.auth;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record LoginSession(
        UUID id,
        UUID userId,
        String sessionTokenId,
        boolean active,
        Instant expiresAt,
        Instant lastLoginAt,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant updatedAt) {

    public LoginSession {
        if (userId == null || sessionTokenId == null || sessionTokenId.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "로그인 세션 식별 정보는 필수입니다.");
        }
        if (expiresAt == null || lastLoginAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "세션 만료와 로그인 시각은 필수입니다.");
        }
    }

    public boolean isExpired(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "기준 시각은 필수입니다.");
        }
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return active && !isExpired(now);
    }

    public LoginSession deactivate() {
        return new LoginSession(
                id,
                userId,
                sessionTokenId,
                false,
                expiresAt,
                lastLoginAt,
                ipAddress,
                userAgent,
                createdAt,
                updatedAt);
    }
}
