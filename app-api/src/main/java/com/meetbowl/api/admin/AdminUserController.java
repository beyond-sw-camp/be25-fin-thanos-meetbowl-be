package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminResetPasswordResponse;
import com.meetbowl.api.admin.dto.AdminUserCreateRequest;
import com.meetbowl.api.admin.dto.AdminUserCreateResponse;
import com.meetbowl.api.admin.dto.AdminUserListResponse;
import com.meetbowl.api.admin.dto.AdminUserResponse;
import com.meetbowl.api.admin.dto.AdminUserStatusUpdateRequest;
import com.meetbowl.api.admin.dto.AdminUserUpdateRequest;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminUserManagementUseCase;
import com.meetbowl.application.admin.ResetUserPasswordCommand;
import com.meetbowl.application.admin.ResetUserPasswordResult;
import com.meetbowl.application.admin.ResetUserPasswordUseCase;
import com.meetbowl.common.response.ApiResponse;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/users")
public class AdminUserController extends BaseController {

    private static final String USER_ID_PATH = "/{userId:[0-9a-fA-F-]{36}}";

    private final AdminUserManagementUseCase adminUserManagementUseCase;
    private final ResetUserPasswordUseCase resetUserPasswordUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public AdminUserController(
            AdminUserManagementUseCase adminUserManagementUseCase,
            ResetUserPasswordUseCase resetUserPasswordUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.adminUserManagementUseCase = adminUserManagementUseCase;
        this.resetUserPasswordUseCase = resetUserPasswordUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @PostMapping
    public ApiResponse<AdminUserCreateResponse> create(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody AdminUserCreateRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserCreateResponse.from(
                        adminUserManagementUseCase.create(
                                new AdminUserManagementUseCase.CreateCommand(
                                        request.loginId(),
                                        request.name(),
                                        request.email(),
                                        request.status().name(),
                                        request.affiliateId(),
                                        request.departmentId(),
                                        request.teamId(),
                                        request.positionId(),
                                        request.activeFrom(),
                                        request.activeUntil(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    @GetMapping
    public ApiResponse<AdminUserListResponse> list(
            @CurrentUser AuthenticatedUser admin,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        requireAdmin(admin);
        return ok(
                AdminUserListResponse.from(
                        adminUserManagementUseCase.search(
                                new AdminUserManagementUseCase.SearchCommand(
                                        keyword, page, size))));
    }

    @GetMapping(USER_ID_PATH)
    public ApiResponse<AdminUserResponse> get(
            @CurrentUser AuthenticatedUser admin, @PathVariable UUID userId) {
        requireAdmin(admin);
        return ok(AdminUserResponse.from(adminUserManagementUseCase.get(userId)));
    }

    @PatchMapping(USER_ID_PATH)
    public ApiResponse<AdminUserResponse> update(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserResponse.from(
                        adminUserManagementUseCase.update(
                                new AdminUserManagementUseCase.UpdateCommand(
                                        userId,
                                        request.name(),
                                        request.email(),
                                        request.affiliateId(),
                                        request.departmentId(),
                                        request.teamId(),
                                        request.positionId(),
                                        request.activeFrom(),
                                        request.activeUntil(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    @PatchMapping(USER_ID_PATH + "/status")
    public ApiResponse<AdminUserResponse> updateStatus(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserResponse.from(
                        adminUserManagementUseCase.updateStatus(
                                new AdminUserManagementUseCase.UpdateStatusCommand(
                                        userId,
                                        request.status().name(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    @PostMapping(USER_ID_PATH + "/password/reset")
    public ApiResponse<AdminResetPasswordResponse> resetPassword(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);

        ResetUserPasswordResult result =
                resetUserPasswordUseCase.execute(
                        new ResetUserPasswordCommand(
                                userId,
                                admin.userId(),
                                admin.displayName(),
                                httpServletRequest.getRemoteAddr(),
                                httpServletRequest.getHeader("User-Agent")));

        return ok(new AdminResetPasswordResponse(result.temporaryPassword()));
    }

    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
