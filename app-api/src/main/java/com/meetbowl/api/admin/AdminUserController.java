package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminResetPasswordResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.ResetUserPasswordCommand;
import com.meetbowl.application.admin.ResetUserPasswordResult;
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
    public ApiResponse<AdminResetPasswordResponse> resetPassword(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            HttpServletRequest httpServletRequest) {

        // 다른 사용자의 비밀번호 초기화는 관리자만 수행할 수 있다.
        globalPermissionChecker.requireAdmin(admin);

        // 유즈케이스는 임시 비밀번호를 한 번만 반환한다.
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
}
