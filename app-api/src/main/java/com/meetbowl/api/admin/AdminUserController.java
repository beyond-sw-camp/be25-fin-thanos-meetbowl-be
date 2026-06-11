package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminResetPasswordRequest;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.ResetUserPasswordCommand;
import com.meetbowl.application.admin.ResetUserPasswordUseCase;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/users")
public class AdminUserController extends BaseController {

    private final ResetUserPasswordUseCase resetUserPasswordUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public AdminUserController(
            ResetUserPasswordUseCase resetUserPasswordUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.resetUserPasswordUseCase = resetUserPasswordUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @PostMapping("/{userId}/password/reset")
    public ApiResponse<Void> resetPassword(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminResetPasswordRequest request,
            HttpServletRequest httpServletRequest) {

        globalPermissionChecker.requireAdmin(admin);

        resetUserPasswordUseCase.execute(
                new ResetUserPasswordCommand(
                        userId,
                        request.newPassword(),
                        admin.userId(),
                        admin.displayName(),
                        httpServletRequest.getRemoteAddr(),
                        httpServletRequest.getHeader("User-Agent")));

        return ok();
    }
}
