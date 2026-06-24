package com.meetbowl.api.admin.dto;

import java.util.List;

import com.meetbowl.application.admin.PasswordResetRequestResult;

public record AdminPasswordResetRequestListResponse(List<AdminPasswordResetRequestResponse> items) {

    public static AdminPasswordResetRequestListResponse from(List<PasswordResetRequestResult> results) {
        return new AdminPasswordResetRequestListResponse(
                results.stream().map(AdminPasswordResetRequestResponse::from).toList());
    }
}
