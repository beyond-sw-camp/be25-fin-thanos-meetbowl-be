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
        // 로그인 응답에는 초기 비밀번호 변경 필요 여부를 함께 내려준다.
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
    public ApiResponse<Void> logout(
            @CurrentUser AuthenticatedUser currentUser, @Valid @RequestBody LogoutRequest request) {
        logoutUseCase.execute(
                new LogoutCommand(
                        currentUser.userId(),
                        request.refreshToken(),
                        currentUser.accessTokenId(),
                        currentUser.accessTokenExpiresAt()));
        return ok();
    }

    @PostMapping("/password/change-initial")
    public ApiResponse<TokenResponse> changeInitialPassword(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangeInitialPasswordRequest request) {
        // 이 엔드포인트는 제한 로그인 흐름을 끝내고 정상 토큰을 발급한다.
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
