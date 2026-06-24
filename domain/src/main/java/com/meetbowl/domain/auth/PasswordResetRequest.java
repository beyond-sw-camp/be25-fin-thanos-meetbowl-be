package com.meetbowl.domain.auth;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record PasswordResetRequest(
        UUID id,
        UUID userId,
        String requesterName,
        String loginId,
        String email,
        PasswordResetRequestStatus status,
        Instant requestedAt,
        Instant processedAt,
        UUID processedByAdminId,
        Instant createdAt,
        Instant updatedAt) {

    public PasswordResetRequest {
        if (id == null || userId == null || status == null || requestedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "비밀번호 초기화 요청 필수 값이 누락되었습니다.");
        }
        validateText(requesterName, "요청자 이름은 필수입니다.");
        validateText(loginId, "로그인 ID는 필수입니다.");
        validateText(email, "이메일은 필수입니다.");
    }

    public static PasswordResetRequest create(
            UUID id, UUID userId, String requesterName, String loginId, String email, Instant requestedAt) {
        return new PasswordResetRequest(
                id,
                userId,
                requesterName,
                loginId,
                email,
                PasswordResetRequestStatus.PENDING,
                requestedAt,
                null,
                null,
                requestedAt,
                requestedAt);
    }

    public PasswordResetRequest approve(UUID adminId, Instant processedAt) {
        ensurePending();
        if (adminId == null || processedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "승인 처리 필수 값이 누락되었습니다.");
        }
        return new PasswordResetRequest(
                id,
                userId,
                requesterName,
                loginId,
                email,
                PasswordResetRequestStatus.APPROVED,
                requestedAt,
                processedAt,
                adminId,
                createdAt,
                updatedAt);
    }

    public PasswordResetRequest reject(UUID adminId, Instant processedAt) {
        ensurePending();
        if (adminId == null || processedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "거절 처리 필수 값이 누락되었습니다.");
        }
        return new PasswordResetRequest(
                id,
                userId,
                requesterName,
                loginId,
                email,
                PasswordResetRequestStatus.REJECTED,
                requestedAt,
                processedAt,
                adminId,
                createdAt,
                updatedAt);
    }

    public boolean isPending() {
        return status == PasswordResetRequestStatus.PENDING;
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_RESET_REQUEST_ALREADY_PROCESSED,
                    "이미 처리된 비밀번호 초기화 요청입니다.");
        }
    }

    private static void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }
}
