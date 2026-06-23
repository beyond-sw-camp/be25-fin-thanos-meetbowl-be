package com.meetbowl.api.admin.dto;

import com.meetbowl.application.admin.AdminOrganizationMembersExcelUseCase;

public record AdminOrganizationMembersExcelImportResponse(
        int createdAffiliates,
        int updatedAffiliates,
        int createdDepartments,
        int updatedDepartments,
        int createdTeams,
        int updatedTeams,
        int createdPositions,
        int updatedPositions,
        int createdUsers,
        int updatedUsers) {

    public static AdminOrganizationMembersExcelImportResponse from(
            AdminOrganizationMembersExcelUseCase.ImportResult result) {
        return new AdminOrganizationMembersExcelImportResponse(
                result.createdAffiliates(),
                result.updatedAffiliates(),
                result.createdDepartments(),
                result.updatedDepartments(),
                result.createdTeams(),
                result.updatedTeams(),
                result.createdPositions(),
                result.updatedPositions(),
                result.createdUsers(),
                result.updatedUsers());
    }
}
