package com.meetbowl.api.auth;

import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.auth.dto.LoginRequest;
import com.meetbowl.api.auth.dto.LoginResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.application.auth.LoginCommand;
import com.meetbowl.application.auth.LoginResult;
import com.meetbowl.application.auth.LoginUseCase;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/auth")
public class AuthController extends BaseController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result =
                loginUseCase.execute(new LoginCommand(request.loginId(), request.password()));

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
    public ApiResponse<Void> logout() {
        return ok();
    }
}
