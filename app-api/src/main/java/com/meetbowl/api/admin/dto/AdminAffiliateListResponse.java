package com.meetbowl.api.admin.dto;

import java.util.List;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminAffiliateListResponse(List<AdminAffiliateResponse> items) {

    public static AdminAffiliateListResponse from(
            List<AdminOrganizationMasterDataUseCase.AffiliateResult> results) {
        return new AdminAffiliateListResponse(AdminAffiliateResponse.from(results));
    }
}
