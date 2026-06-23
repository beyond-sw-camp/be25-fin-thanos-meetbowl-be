package com.meetbowl.api.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.MailRetentionPolicyRequest;
import com.meetbowl.api.admin.dto.MailRetentionPolicyResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.mail.MailRetentionPolicyCommand;
import com.meetbowl.application.mail.MailRetentionPolicyUseCase;
import com.meetbowl.common.response.ApiResponse;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/mail/retention-policy")
public class MailRetentionPolicyController extends BaseController {

    private final MailRetentionPolicyUseCase mailRetentionPolicyUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public MailRetentionPolicyController(
            MailRetentionPolicyUseCase mailRetentionPolicyUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.mailRetentionPolicyUseCase = mailRetentionPolicyUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping
    public ApiResponse<MailRetentionPolicyResponse> get(@CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(MailRetentionPolicyResponse.from(mailRetentionPolicyUseCase.get()));
    }

    @PatchMapping
    public ApiResponse<MailRetentionPolicyResponse> update(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody MailRetentionPolicyRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                MailRetentionPolicyResponse.from(
                        mailRetentionPolicyUseCase.update(
                                new MailRetentionPolicyCommand(
                                        request.retentionDays(),
                                        request.autoDeleteEnabled(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
