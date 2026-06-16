package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminTeamResponse(
        UUID teamId,
        UUID departmentId,
        String name,
        String code,
        String status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminTeamResponse from(AdminOrganizationMasterDataUseCase.TeamResult result) {
        return new AdminTeamResponse(
                result.teamId(),
                result.departmentId(),
                result.name(),
                result.code(),
                result.status(),
                result.sortOrder(),
                result.createdAt(),
                result.updatedAt());
    }

    public static List<AdminTeamResponse> from(
            List<AdminOrganizationMasterDataUseCase.TeamResult> results) {
        return results.stream().map(AdminTeamResponse::from).toList();
    }
}
