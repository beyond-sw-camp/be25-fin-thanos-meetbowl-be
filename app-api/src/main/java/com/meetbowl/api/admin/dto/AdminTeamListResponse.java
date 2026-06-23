package com.meetbowl.api.admin.dto;

import java.util.List;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminTeamListResponse(List<AdminTeamResponse> items) {

    public static AdminTeamListResponse from(
            List<AdminOrganizationMasterDataUseCase.TeamResult> results) {
        return new AdminTeamListResponse(AdminTeamResponse.from(results));
    }
}
