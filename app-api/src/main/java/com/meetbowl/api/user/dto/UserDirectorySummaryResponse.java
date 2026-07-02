package com.meetbowl.api.user.dto;

import java.util.List;
import java.util.UUID;

import com.meetbowl.application.user.UserDirectoryUseCase;

public record UserDirectorySummaryResponse(
        UUID userId,
        String loginId,
        String name,
        String email,
        String role,
        String status,
        UUID affiliateId,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        String affiliate,
        String department,
        String team,
        String position) {

    public static UserDirectorySummaryResponse from(
            UserDirectoryUseCase.UserDirectorySummary result) {
        return new UserDirectorySummaryResponse(
                result.userId(),
                result.loginId(),
                result.name(),
                result.email(),
                result.role(),
                result.status(),
                result.affiliateId(),
                result.departmentId(),
                result.teamId(),
                result.positionId(),
                result.affiliate(),
                result.department(),
                result.team(),
                result.position());
    }

    public static List<UserDirectorySummaryResponse> from(
            List<UserDirectoryUseCase.UserDirectorySummary> results) {
        return results.stream().map(UserDirectorySummaryResponse::from).toList();
    }
}
