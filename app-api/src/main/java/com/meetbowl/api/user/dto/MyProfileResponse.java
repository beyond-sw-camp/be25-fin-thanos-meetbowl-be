package com.meetbowl.api.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.user.MyProfileResult;

public record MyProfileResponse(
        UUID userId,
        String loginId,
        String name,
        String email,
        String role,
        String status,
        String affiliate,
        String department,
        String team,
        String position,
        Instant activeFrom,
        Instant activeUntil) {

    public static MyProfileResponse from(MyProfileResult result) {
        return new MyProfileResponse(
                result.userId(),
                result.loginId(),
                result.name(),
                result.email(),
                result.role(),
                result.status(),
                result.affiliate(),
                result.department(),
                result.team(),
                result.position(),
                result.activeFrom(),
                result.activeUntil());
    }
}
