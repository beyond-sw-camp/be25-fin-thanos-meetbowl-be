package com.meetbowl.api.auth;

import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.auth.dto.ChangeInitialPasswordRequest;
import com.meetbowl.api.auth.dto.LoginRequest;
import com.meetbowl.api.auth.dto.LoginResponse;
import com.meetbowl.api.auth.dto.LogoutRequest;
import com.meetbowl.api.auth.dto.RefreshTokenRequest;
import com.meetbowl.api.auth.dto.TokenResponse;
import com.meetbowl.api.common.ApiHeaders;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.application.auth.ChangeInitialPasswordCommand;
import com.meetbowl.application.auth.ChangeInitialPasswordUseCase;
import com.meetbowl.application.auth.IssuedTokens;
import com.meetbowl.application.auth.LoginCommand;
import com.meetbowl.application.auth.LoginResult;
import com.meetbowl.application.auth.LoginUseCase;
import com.meetbowl.application.auth.LogoutCommand;
import com.meetbowl.application.auth.LogoutUseCase;
import com.meetbowl.application.auth.RefreshTokenCommand;
import com.meetbowl.application.auth.RefreshTokenUseCase;
import com.meetbowl.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
public class AuthController extends BaseController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ChangeInitialPasswordUseCase changeInitialPasswordUseCase;

    public AuthController(
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            ChangeInitialPasswordUseCase changeInitialPasswordUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.changeInitialPasswordUseCase = changeInitialPasswordUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result =
                loginUseCase.execute(new LoginCommand(request.loginId(), request.password()));

        return ok(
                new LoginResponse(
                        result.accessToken(),
                        result.refreshToken(),
                        result.tokenType(),
                        result.accessTokenExpiresIn(),
                        result.refreshTokenExpiresIn(),
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
                                result.user().position(),
                                result.user().initialPasswordChangeRequired())));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        IssuedTokens tokens =
                refreshTokenUseCase.execute(new RefreshTokenCommand(request.refreshToken()));
        return ok(TokenResponse.from(tokens));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = ApiHeaders.AUTHORIZATION)
    public ApiResponse<Void> logout(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody LogoutRequest request) {
        logoutUseCase.execute(
                new LogoutCommand(
                        currentUser.userId(),
                        request.refreshToken(),
                        currentUser.accessTokenId(),
                        currentUser.accessTokenExpiresAt()));
        return ok();
    }

    @PostMapping("/password/change-initial")
    @SecurityRequirement(name = ApiHeaders.AUTHORIZATION)
    public ApiResponse<TokenResponse> changeInitialPassword(
            @Parameter(hidden = true) @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangeInitialPasswordRequest request) {
        IssuedTokens tokens =
                changeInitialPasswordUseCase.execute(
                        new ChangeInitialPasswordCommand(
                                currentUser.userId(),
                                request.newPassword(),
                                currentUser.accessTokenId(),
                                currentUser.accessTokenExpiresAt()));
        return ok(TokenResponse.from(tokens));
    }
}
