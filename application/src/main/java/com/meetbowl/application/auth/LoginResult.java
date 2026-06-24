package com.meetbowl.application.auth;

import java.util.UUID;

public record LoginResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        UserSummary user) {

    public record UserSummary(
            UUID userId,
            String loginId,
            String name,
            String email,
            String role,
            String status,
            UUID affiliateId,
            String affiliate,
            String department,
            String team,
            String position,
            boolean initialPasswordChangeRequired) {}
}
