package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.auth.PasswordResetRequest;

public record PasswordResetRequestResult(
        UUID requestId,
        String requesterName,
        String loginId,
        String email,
        Instant requestedAt,
        String status,
        Instant processedAt) {

    public static PasswordResetRequestResult from(PasswordResetRequest request) {
        return new PasswordResetRequestResult(
                request.id(),
                request.requesterName(),
                request.loginId(),
                request.email(),
                request.requestedAt(),
                request.status().name(),
                request.processedAt());
    }
}
