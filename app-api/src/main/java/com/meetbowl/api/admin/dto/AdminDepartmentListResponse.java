package com.meetbowl.api.admin.dto;

import java.util.List;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminDepartmentListResponse(List<AdminDepartmentResponse> items) {

    public static AdminDepartmentListResponse from(
            List<AdminOrganizationMasterDataUseCase.DepartmentResult> results) {
        return new AdminDepartmentListResponse(AdminDepartmentResponse.from(results));
    }
}
