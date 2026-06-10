package com.meetbowl.api.auth;

import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.auth.dto.LoginRequest;
import com.meetbowl.api.auth.dto.LoginResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.application.auth.LoginCommand;
import com.meetbowl.application.auth.LoginResult;
import com.meetbowl.application.auth.LoginUseCase;
import com.meetbowl.application.auth.LogoutCommand;
import com.meetbowl.application.auth.LogoutUseCase;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
public class AuthController extends BaseController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(LoginUseCase loginUseCase, LogoutUseCase logoutUseCase) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginResult result =
                loginUseCase.execute(
                        new LoginCommand(
                                request.loginId(),
                                request.password(),
                                servletRequest.getRemoteAddr(),
                                servletRequest.getHeader("User-Agent")));

        return ok(
                new LoginResponse(
                        result.accessToken(),
                        result.tokenType(),
                        result.expiresAt(),
                        new LoginResponse.UserSummary(
                                result.user().userId(),
                                result.user().loginId(),
                                result.user().name(),
                                result.user().email(),
                                Objects.toString(result.user().role(), null),
                                Objects.toString(result.user().status(), null),
                                result.user().affiliate(),
                                result.user().department(),
                                result.user().team(),
                                result.user().position())));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser AuthenticatedUser currentUser) {
        logoutUseCase.execute(new LogoutCommand(currentUser.userId()));
        return ok();
    }
}
