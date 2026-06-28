package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminPositionResponse(
        UUID positionId,
        UUID affiliateId,
        String name,
        String code,
        String status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminPositionResponse from(
            AdminOrganizationMasterDataUseCase.PositionResult result) {
        return new AdminPositionResponse(
                result.positionId(),
                result.affiliateId(),
                result.name(),
                result.code(),
                result.status(),
                result.sortOrder(),
                result.createdAt(),
                result.updatedAt());
    }

    public static List<AdminPositionResponse> from(
            List<AdminOrganizationMasterDataUseCase.PositionResult> results) {
        return results.stream().map(AdminPositionResponse::from).toList();
    }
}
