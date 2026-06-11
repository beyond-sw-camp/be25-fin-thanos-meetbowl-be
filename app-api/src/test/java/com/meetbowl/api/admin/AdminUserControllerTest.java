package com.meetbowl.api.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meetbowl.api.common.GlobalExceptionHandler;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserAttributes;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.common.auth.CurrentUserArgumentResolver;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.config.WebMvcConfig;
import com.meetbowl.application.admin.AdminUserManagementUseCase;
import com.meetbowl.application.admin.ResetUserPasswordUseCase;
import com.meetbowl.application.auth.AccessTokenValidationService;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class AdminUserControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminUserManagementUseCase adminUserManagementUseCase;
    @MockitoBean private ResetUserPasswordUseCase resetUserPasswordUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void createUserSuccess() throws Exception {
        given(adminUserManagementUseCase.create(any())).willReturn(createResult());

        mockMvc.perform(
                        post("/api/v1/users")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminUserControllerTest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "loginId": "user01",
                                          "name": "사용자",
                                          "email": "user01@example.com",
                                          "status": "ACTIVE",
                                          "affiliateId": "%s",
                                          "departmentId": "%s",
                                          "teamId": "%s",
                                          "positionId": "%s",
                                          "activeFrom": "2026-06-11T00:00:00Z",
                                          "activeUntil": "2026-12-31T23:59:59Z"
                                        }
                                        """
                                                .formatted(
                                                        AFFILIATE_ID,
                                                        DEPARTMENT_ID,
                                                        TEAM_ID,
                                                        POSITION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temporaryPassword").value("Temp1234Abcd5678"))
                .andExpect(jsonPath("$.data.user.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.user.loginId").value("user01"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.user.initialPasswordChangeRequired").value(true));

        ArgumentCaptor<AdminUserManagementUseCase.CreateCommand> commandCaptor =
                ArgumentCaptor.forClass(AdminUserManagementUseCase.CreateCommand.class);
        verify(adminUserManagementUseCase).create(commandCaptor.capture());

        AdminUserManagementUseCase.CreateCommand command = commandCaptor.getValue();
        assertEquals("user01", command.loginId());
        assertEquals("user01@example.com", command.email());
        assertEquals("ACTIVE", command.status());
        assertEquals(AFFILIATE_ID, command.affiliateId());
        assertEquals(DEPARTMENT_ID, command.departmentId());
        assertEquals(TEAM_ID, command.teamId());
        assertEquals(POSITION_ID, command.positionId());
        assertEquals(ADMIN_ID, command.adminId());
        assertEquals("관리자", command.adminName());
        assertEquals("AdminUserControllerTest", command.userAgent());
    }

    @Test
    void createUserFailsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "loginId": " ",
                                          "name": "",
                                          "email": "invalid-email",
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(adminUserManagementUseCase);
    }

    @Test
    void createUserFailsWhenNotAdmin() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "loginId": "user01",
                                          "name": "사용자",
                                          "email": "user01@example.com",
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminUserManagementUseCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "관리자");
    }

    private AdminUserManagementUseCase.CreateResult createResult() {
        AdminUserManagementUseCase.UserSummary user =
                new AdminUserManagementUseCase.UserSummary(
                        USER_ID,
                        "user01",
                        "사용자",
                        "user01@example.com",
                        "USER",
                        "ACTIVE",
                        AFFILIATE_ID,
                        "계열사",
                        DEPARTMENT_ID,
                        "부서",
                        TEAM_ID,
                        "팀",
                        POSITION_ID,
                        "직급",
                        Instant.parse("2026-06-11T00:00:00Z"),
                        Instant.parse("2026-12-31T23:59:59Z"),
                        Instant.parse("2026-06-11T00:00:00Z"),
                        Instant.parse("2026-06-11T00:00:00Z"),
                        true);
        return new AdminUserManagementUseCase.CreateResult(USER_ID, "Temp1234Abcd5678", user);
    }
}
