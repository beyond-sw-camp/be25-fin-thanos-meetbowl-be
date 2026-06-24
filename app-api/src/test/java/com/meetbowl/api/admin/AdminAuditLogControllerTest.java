package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.admin.AdminAuditLogPageResult;
import com.meetbowl.application.admin.AdminAuditLogQueryUseCase;
import com.meetbowl.application.admin.AdminAuditLogResult;
import com.meetbowl.application.admin.AdminAuditLogSearchCommand;
import com.meetbowl.application.auth.AccessTokenValidationService;

@WebMvcTest(AdminAuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class AdminAuditLogControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID AUDIT_LOG_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000203");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000204");
    private static final Instant CREATED_AT = Instant.parse("2026-06-12T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminAuditLogQueryUseCase adminAuditLogQueryUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void listAuditLogsSuccessWithFiltersAndPaging() throws Exception {
        given(adminAuditLogQueryUseCase.search(any()))
                .willReturn(new AdminAuditLogPageResult(List.of(result()), 1, 10, 1, 1));

        mockMvc.perform(
                        get("/api/v1/admin/audit-logs")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .param("actorUserId", ADMIN_ID.toString())
                                .param("actorName", "admin01")
                                .param("actionType", "USER_UPDATE")
                                .param("targetType", "USER")
                                .param("targetId", TARGET_ID.toString())
                                .param("result", "SUCCESS")
                                .param("from", "2026-06-01T00:00:00Z")
                                .param("to", "2026-06-13T00:00:00Z")
                                .param("page", "1")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].auditLogId").value(AUDIT_LOG_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].actorUserId").value(ADMIN_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].actorName").value("admin01"))
                .andExpect(jsonPath("$.data.items[0].actionType").value("USER_UPDATE"))
                .andExpect(jsonPath("$.data.items[0].targetType").value("USER"))
                .andExpect(jsonPath("$.data.items[0].targetLoginId").value("user01"))
                .andExpect(jsonPath("$.data.items[0].targetName").value("User One"))
                .andExpect(jsonPath("$.data.items[0].ipAddress").value("203.0.113.10"))
                .andExpect(jsonPath("$.data.items[0].result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].beforeSnapshot.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].afterSnapshot.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        ArgumentCaptor<AdminAuditLogSearchCommand> captor =
                ArgumentCaptor.forClass(AdminAuditLogSearchCommand.class);
        verify(adminAuditLogQueryUseCase).search(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(ADMIN_ID, captor.getValue().actorUserId());
        org.junit.jupiter.api.Assertions.assertEquals("admin01", captor.getValue().actorName());
        org.junit.jupiter.api.Assertions.assertEquals(
                "USER_UPDATE", captor.getValue().actionType());
        org.junit.jupiter.api.Assertions.assertEquals(TARGET_ID, captor.getValue().targetId());
    }

    @Test
    void getAuditLogSuccess() throws Exception {
        given(adminAuditLogQueryUseCase.get(AUDIT_LOG_ID)).willReturn(result());

        mockMvc.perform(
                        get("/api/v1/admin/audit-logs/{auditLogId}", AUDIT_LOG_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditLogId").value(AUDIT_LOG_ID.toString()))
                .andExpect(jsonPath("$.data.targetLoginId").value("user01"))
                .andExpect(jsonPath("$.data.targetName").value("User One"))
                .andExpect(jsonPath("$.data.ipAddress").value("203.0.113.10"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-06-12T00:00:00Z"));
    }

    @Test
    void userRoleCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/audit-logs")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminAuditLogQueryUseCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "admin01");
    }

    private AdminAuditLogResult result() {
        return new AdminAuditLogResult(
                AUDIT_LOG_ID,
                ADMIN_ID,
                "admin01",
                "USER_UPDATE",
                "USER",
                TARGET_ID,
                "user01",
                "User One",
                "203.0.113.10",
                "SUCCESS",
                null,
                "{\"status\":\"ACTIVE\",\"activeFrom\":1781798400,\"activeUntil\":1781971200}",
                "{\"status\":\"INACTIVE\",\"activeFrom\":1781798400,\"activeUntil\":1781971200}",
                CREATED_AT);
    }
}
