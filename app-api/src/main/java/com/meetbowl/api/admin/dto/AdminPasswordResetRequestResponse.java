package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.admin.PasswordResetRequestResult;

public record AdminPasswordResetRequestResponse(
        UUID requestId,
        String requesterName,
        String loginId,
        String email,
        Instant requestedAt,
        String status,
        Instant processedAt) {

    public static AdminPasswordResetRequestResponse from(PasswordResetRequestResult result) {
        return new AdminPasswordResetRequestResponse(
                result.requestId(),
                result.requesterName(),
                result.loginId(),
                result.email(),
                result.requestedAt(),
                result.status(),
                result.processedAt());
    }
}
