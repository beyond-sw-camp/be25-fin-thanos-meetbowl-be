package com.meetbowl.api.admin.dto;

import com.meetbowl.application.admin.AdminUserManagementUseCase;

public record AdminUserCreateResponse(String temporaryPassword, AdminUserResponse user) {

    public static AdminUserCreateResponse from(
            AdminUserManagementUseCase.CreateResult result) {
        return new AdminUserCreateResponse(
                result.temporaryPassword(), AdminUserResponse.from(result.user()));
    }
}
