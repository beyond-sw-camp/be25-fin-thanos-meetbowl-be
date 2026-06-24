package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
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
import com.meetbowl.application.admin.AdminPasswordResetRequestUseCase;
import com.meetbowl.application.admin.PasswordResetRequestResult;
import com.meetbowl.application.auth.AccessTokenValidationService;

@WebMvcTest(PasswordResetRequestAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class PasswordResetRequestAdminControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminPasswordResetRequestUseCase adminPasswordResetRequestUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void listSuccess() throws Exception {
        given(adminPasswordResetRequestUseCase.list("PENDING")).willReturn(List.of(result("PENDING")));

        mockMvc.perform(
                        get("/api/v1/admin/password-reset-requests")
                                .param("status", "PENDING")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].loginId").value("user1"))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"));
    }

    @Test
    void countSuccess() throws Exception {
        given(adminPasswordResetRequestUseCase.countPending()).willReturn(3L);

        mockMvc.perform(
                        get("/api/v1/admin/notifications/count")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingPasswordResetRequestCount").value(3));
    }

    @Test
    void approveSuccess() throws Exception {
        given(adminPasswordResetRequestUseCase.approve(any())).willReturn(result("APPROVED"));

        mockMvc.perform(
                        post("/api/v1/admin/password-reset-requests/{requestId}/approve", REQUEST_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectFailsWhenNotAdmin() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/password-reset-requests/{requestId}/reject", REQUEST_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        new AuthenticatedUser(
                                                ADMIN_ID, ORG_ID, AuthenticatedUserRole.USER, "User")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminPasswordResetRequestUseCase);
    }

    private AuthenticatedUser adminUser() {
        return new AuthenticatedUser(ADMIN_ID, ORG_ID, AuthenticatedUserRole.ADMIN, "Admin");
    }

    private PasswordResetRequestResult result(String status) {
        return new PasswordResetRequestResult(
                REQUEST_ID,
                "User One",
                "user1",
                "user1@example.com",
                Instant.parse("2026-06-23T00:00:00Z"),
                status,
                "PENDING".equals(status) ? null : Instant.parse("2026-06-23T00:05:00Z"));
    }
}
