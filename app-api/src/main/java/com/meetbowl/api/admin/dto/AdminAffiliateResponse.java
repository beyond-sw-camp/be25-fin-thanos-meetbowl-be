package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminAffiliateResponse(
        UUID affiliateId,
        String name,
        String code,
        String status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminAffiliateResponse from(
            AdminOrganizationMasterDataUseCase.AffiliateResult result) {
        return new AdminAffiliateResponse(
                result.affiliateId(),
                result.name(),
                result.code(),
                result.status(),
                result.sortOrder(),
                result.createdAt(),
                result.updatedAt());
    }

    public static List<AdminAffiliateResponse> from(
            List<AdminOrganizationMasterDataUseCase.AffiliateResult> results) {
        return results.stream().map(AdminAffiliateResponse::from).toList();
    }
}
