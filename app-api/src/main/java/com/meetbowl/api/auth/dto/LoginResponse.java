package com.meetbowl.api.auth.dto;

import java.util.UUID;

public record LoginResponse(
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
            String affiliate,
            String department,
            String team,
            String position) {}
}
