package com.meetbowl.api.admin;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.api.admin.dto.AdminAuditLogListResponse;
import com.meetbowl.api.admin.dto.AdminAuditLogResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminAuditLogQueryUseCase;
import com.meetbowl.application.admin.AdminAuditLogSearchCommand;
import com.meetbowl.common.response.ApiResponse;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/audit-logs")
public class AdminAuditLogController extends BaseController {

    private final AdminAuditLogQueryUseCase adminAuditLogQueryUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;
    private final ObjectMapper objectMapper;

    public AdminAuditLogController(
            AdminAuditLogQueryUseCase adminAuditLogQueryUseCase,
            GlobalPermissionChecker globalPermissionChecker,
            ObjectMapper objectMapper) {
        this.adminAuditLogQueryUseCase = adminAuditLogQueryUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<AdminAuditLogListResponse> list(
            @CurrentUser AuthenticatedUser admin,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String actorName,
            @RequestParam(required = false) String actorLoginId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        requireAdmin(admin);
        return ok(
                AdminAuditLogListResponse.from(
                        adminAuditLogQueryUseCase.search(
                                new AdminAuditLogSearchCommand(
                                        actorUserId,
                                        actorName != null ? actorName : actorLoginId,
                                        actionType,
                                        targetType,
                                        targetId,
                                        result,
                                        from,
                                        to,
                                        page,
                                        size)),
                        objectMapper));
    }

    @GetMapping("/{auditLogId:[0-9a-fA-F-]{36}}")
    public ApiResponse<AdminAuditLogResponse> get(
            @CurrentUser AuthenticatedUser admin, @PathVariable UUID auditLogId) {
        requireAdmin(admin);
        return ok(
                AdminAuditLogResponse.from(
                        adminAuditLogQueryUseCase.get(auditLogId), objectMapper));
    }

    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
