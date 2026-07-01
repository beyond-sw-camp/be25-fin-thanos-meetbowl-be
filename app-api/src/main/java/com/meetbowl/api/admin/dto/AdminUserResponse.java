package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.admin.AdminUserManagementUseCase;

public record AdminUserResponse(
        UUID userId,
        String loginId,
        String name,
        String email,
        String role,
        String status,
        UUID affiliateId,
        String affiliate,
        UUID departmentId,
        String department,
        UUID teamId,
        String team,
        UUID positionId,
        String position,
        Instant activeFrom,
        Instant activeUntil,
        Instant createdAt,
        Instant updatedAt,
        boolean initialPasswordChangeRequired) {

    public static AdminUserResponse from(AdminUserManagementUseCase.UserSummary summary) {
        return new AdminUserResponse(
                summary.userId(),
                summary.loginId(),
                summary.name(),
                summary.email(),
                summary.role(),
                summary.status(),
                summary.affiliateId(),
                summary.affiliate(),
                summary.departmentId(),
                summary.department(),
                summary.teamId(),
                summary.team(),
                summary.positionId(),
                summary.position(),
                summary.activeFrom(),
                summary.activeUntil(),
                summary.createdAt(),
                summary.updatedAt(),
                summary.initialPasswordChangeRequired());
    }
}
