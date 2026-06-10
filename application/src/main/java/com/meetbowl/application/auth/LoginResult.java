package com.meetbowl.application.auth;

import java.util.UUID;

import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

public record LoginResult(String accessToken, String tokenType, long expiresAt, UserSummary user) {

    public record UserSummary(
            UUID userId,
            String loginId,
            String name,
            String email,
            UserRole role,
            UserStatus status,
            String affiliate,
            String department,
            String team,
            String position) {}
}
