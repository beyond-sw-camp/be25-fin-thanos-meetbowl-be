package com.meetbowl.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.auth.dto.PasswordResetRequest;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.application.auth.PasswordResetRequestCommand;
import com.meetbowl.application.auth.PasswordResetRequestUseCase;
import com.meetbowl.common.response.ApiResponse;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/password-reset-requests")
public class PasswordResetRequestController extends BaseController {

    private final PasswordResetRequestUseCase passwordResetRequestUseCase;

    public PasswordResetRequestController(PasswordResetRequestUseCase passwordResetRequestUseCase) {
        this.passwordResetRequestUseCase = passwordResetRequestUseCase;
    }

    @PostMapping
    public ApiResponse<Void> create(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpServletRequest) {
        passwordResetRequestUseCase.execute(
                new PasswordResetRequestCommand(
                        request.loginId(),
                        request.email(),
                        httpServletRequest.getRemoteAddr(),
                        httpServletRequest.getHeader("User-Agent")));
        return ok(null, PasswordResetRequestUseCase.ACCEPTED_MESSAGE);
    }
}
