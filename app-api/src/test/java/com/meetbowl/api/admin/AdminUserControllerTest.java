package com.meetbowl.api.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.meetbowl.application.admin.AdminUserSearchIndexUseCase;
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
    @MockitoBean private AdminUserSearchIndexUseCase adminUserSearchIndexUseCase;
    @MockitoBean private ResetUserPasswordUseCase resetUserPasswordUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void createUserSuccess() throws Exception {
        given(adminUserManagementUseCase.create(any())).willReturn(createResult("ADMIN"));

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminUserControllerTest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "loginId": "user01",
                                          "name": "User One",
                                          "email": "user01@example.com",
                                          "role": "ADMIN",
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
                .andExpect(jsonPath("$.data.temporaryPassword").value("1234"))
                .andExpect(jsonPath("$.data.user.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.user.loginId").value("user01"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.user.initialPasswordChangeRequired").value(true));

        ArgumentCaptor<AdminUserManagementUseCase.CreateCommand> commandCaptor =
                ArgumentCaptor.forClass(AdminUserManagementUseCase.CreateCommand.class);
        verify(adminUserManagementUseCase).create(commandCaptor.capture());
        assertEquals("ADMIN", commandCaptor.getValue().role());
        assertEquals(ADMIN_ID, commandCaptor.getValue().adminId());
    }

    @Test
    void listUsersSuccessIncludesTotalPages() throws Exception {
        given(adminUserManagementUseCase.search(any()))
                .willReturn(
                        new AdminUserManagementUseCase.PageResult(
                                List.of(userSummary("USER")), 3, 1, 2, 2));

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .param("keyword", " admin ")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].role").value("USER"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        ArgumentCaptor<AdminUserManagementUseCase.SearchCommand> commandCaptor =
                ArgumentCaptor.forClass(AdminUserManagementUseCase.SearchCommand.class);
        verify(adminUserManagementUseCase).search(commandCaptor.capture());
        assertEquals(1, commandCaptor.getValue().page());
        assertEquals(20, commandCaptor.getValue().size());
        assertEquals(" admin ", commandCaptor.getValue().keyword());
    }

    @Test
    void getUserSuccess() throws Exception {
        given(adminUserManagementUseCase.get(USER_ID)).willReturn(userSummary("ADMIN"));

        mockMvc.perform(
                        get("/api/v1/admin/users/{userId}", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void reindexSearchDocumentsSuccess() throws Exception {
        given(adminUserSearchIndexUseCase.reindexAll())
                .willReturn(new AdminUserSearchIndexUseCase.ReindexResult(12, 1));

        mockMvc.perform(
                        post("/api/v1/admin/users/search-index/reindex")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processedCount").value(12))
                .andExpect(jsonPath("$.data.failedCount").value(1));
    }

    @Test
    void updateUserSuccess() throws Exception {
        given(adminUserManagementUseCase.update(any())).willReturn(userSummary("ADMIN"));

        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminUserControllerTest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Updated User",
                                          "email": "updated@example.com",
                                          "role": "ADMIN",
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
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        ArgumentCaptor<AdminUserManagementUseCase.UpdateCommand> commandCaptor =
                ArgumentCaptor.forClass(AdminUserManagementUseCase.UpdateCommand.class);
        verify(adminUserManagementUseCase).update(commandCaptor.capture());
        assertEquals("ADMIN", commandCaptor.getValue().role());
        assertEquals(USER_ID, commandCaptor.getValue().userId());
    }

    @Test
    void updateStatusActiveSuccess() throws Exception {
        given(adminUserManagementUseCase.updateStatus(any()))
                .willReturn(userSummary("USER", "ACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/status", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminUserControllerTest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void updateStatusInactiveSuccess() throws Exception {
        given(adminUserManagementUseCase.updateStatus(any()))
                .willReturn(userSummary("USER", "INACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/status", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminUserControllerTest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void deleteUserSuccess() throws Exception {
        given(adminUserManagementUseCase.delete(any())).willReturn(userSummary("USER", "INACTIVE"));

        mockMvc.perform(
                        delete("/api/v1/admin/users/{userId}", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminUserControllerTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        ArgumentCaptor<AdminUserManagementUseCase.DeleteCommand> commandCaptor =
                ArgumentCaptor.forClass(AdminUserManagementUseCase.DeleteCommand.class);
        verify(adminUserManagementUseCase).delete(commandCaptor.capture());
        assertEquals(USER_ID, commandCaptor.getValue().userId());
        assertEquals(ADMIN_ID, commandCaptor.getValue().adminId());
    }

    @Test
    void updateStatusLockedFailsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/status", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_REQUEST"));

        verifyNoInteractions(adminUserManagementUseCase);
    }

    @Test
    void updateStatusFailsWhenNotAdmin() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/users/{userId}/status", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminUserManagementUseCase);
    }

    @Test
    void deleteFailsWhenNotAdmin() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/admin/users/{userId}", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminUserManagementUseCase);
    }

    @Test
    void createUserFailsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/users")
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
                        post("/api/v1/admin/users")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "loginId": "user01",
                                          "name": "User One",
                                          "email": "user01@example.com",
                                          "role": "USER",
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(adminUserManagementUseCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "Admin");
    }

    private AdminUserManagementUseCase.CreateResult createResult(String role) {
        return new AdminUserManagementUseCase.CreateResult(
                USER_ID, "1234", userSummary(role, "ACTIVE"));
    }

    private AdminUserManagementUseCase.UserSummary userSummary(String role) {
        return userSummary(role, "ACTIVE");
    }

    private AdminUserManagementUseCase.UserSummary userSummary(String role, String status) {
        return new AdminUserManagementUseCase.UserSummary(
                USER_ID,
                "user01",
                "User One",
                "user01@example.com",
                role,
                status,
                AFFILIATE_ID,
                "Affiliate",
                DEPARTMENT_ID,
                "Department",
                TEAM_ID,
                "Team",
                POSITION_ID,
                "Position",
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"),
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z"),
                true);
    }
}
