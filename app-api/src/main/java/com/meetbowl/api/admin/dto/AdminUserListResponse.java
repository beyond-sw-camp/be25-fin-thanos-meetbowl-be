package com.meetbowl.api.admin.dto;

import java.util.List;

import com.meetbowl.application.admin.AdminUserManagementUseCase;

public record AdminUserListResponse(
        List<AdminUserResponse> items, long totalElements, int page, int size) {

    public static AdminUserListResponse from(AdminUserManagementUseCase.PageResult result) {
        return new AdminUserListResponse(
                result.items().stream().map(AdminUserResponse::from).toList(),
                result.totalElements(),
                result.page(),
                result.size());
    }
}
