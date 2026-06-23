package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;

public record AdminDepartmentResponse(
        UUID departmentId,
        UUID affiliateId,
        String name,
        String code,
        String status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminDepartmentResponse from(
            AdminOrganizationMasterDataUseCase.DepartmentResult result) {
        return new AdminDepartmentResponse(
                result.departmentId(),
                result.affiliateId(),
                result.name(),
                result.code(),
                result.status(),
                result.sortOrder(),
                result.createdAt(),
                result.updatedAt());
    }

    public static List<AdminDepartmentResponse> from(
            List<AdminOrganizationMasterDataUseCase.DepartmentResult> results) {
        return results.stream().map(AdminDepartmentResponse::from).toList();
    }
}
