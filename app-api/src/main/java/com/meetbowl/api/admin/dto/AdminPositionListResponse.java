package com.meetbowl.api.admin.dto;

import java.util.List;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminPositionListResponse(List<AdminPositionResponse> items) {

    public static AdminPositionListResponse from(
            List<AdminOrganizationMasterDataUseCase.PositionResult> results) {
        return new AdminPositionListResponse(AdminPositionResponse.from(results));
    }
}
