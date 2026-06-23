package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminNotificationCountResponse;
import com.meetbowl.api.admin.dto.AdminPasswordResetRequestListResponse;
import com.meetbowl.api.admin.dto.AdminPasswordResetRequestResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminPasswordResetRequestUseCase;
import com.meetbowl.application.admin.AdminPasswordResetRequestUseCase.DecisionCommand;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin")
public class PasswordResetRequestAdminController extends BaseController {

    private final AdminPasswordResetRequestUseCase adminPasswordResetRequestUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public PasswordResetRequestAdminController(
            AdminPasswordResetRequestUseCase adminPasswordResetRequestUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.adminPasswordResetRequestUseCase = adminPasswordResetRequestUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping("/password-reset-requests")
    public ApiResponse<AdminPasswordResetRequestListResponse> list(
            @CurrentUser AuthenticatedUser admin,
            @RequestParam(required = false) String status) {
        requireAdmin(admin);
        return ok(
                AdminPasswordResetRequestListResponse.from(
                        adminPasswordResetRequestUseCase.list(status)));
    }

    @GetMapping("/notifications/count")
    public ApiResponse<AdminNotificationCountResponse> count(@CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(
                new AdminNotificationCountResponse(
                        adminPasswordResetRequestUseCase.countPending()));
    }

    @PostMapping("/password-reset-requests/{requestId}/approve")
    public ApiResponse<AdminPasswordResetRequestResponse> approve(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID requestId,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminPasswordResetRequestResponse.from(
                        adminPasswordResetRequestUseCase.approve(
                                decisionCommand(admin, requestId, httpServletRequest))));
    }

    @PostMapping("/password-reset-requests/{requestId}/reject")
    public ApiResponse<AdminPasswordResetRequestResponse> reject(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID requestId,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminPasswordResetRequestResponse.from(
                        adminPasswordResetRequestUseCase.reject(
                                decisionCommand(admin, requestId, httpServletRequest))));
    }

    private DecisionCommand decisionCommand(
            AuthenticatedUser admin, UUID requestId, HttpServletRequest httpServletRequest) {
        return new DecisionCommand(
                requestId,
                admin.userId(),
                admin.displayName(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent"));
    }

    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
