package com.meetbowl.api.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.application.user.UserDirectoryUseCase;

@WebMvcTest(UserDirectoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class UserDirectoryControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserDirectoryUseCase userDirectoryUseCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void searchByNameSuccess() throws Exception {
        given(userDirectoryUseCase.search(any()))
                .willReturn(new UserDirectoryUseCase.PageResult(List.of(summary()), 1, 20, 1, 1));

        mockMvc.perform(
                        get("/api/v1/users/search")
                                .param("keyword", " Hong ")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Hong Gil Dong"))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());

        ArgumentCaptor<UserDirectoryUseCase.SearchCommand> commandCaptor =
                ArgumentCaptor.forClass(UserDirectoryUseCase.SearchCommand.class);
        org.mockito.Mockito.verify(userDirectoryUseCase).search(commandCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(" Hong ", commandCaptor.getValue().keyword());
    }

    @Test
    void searchByEmailSuccess() throws Exception {
        given(userDirectoryUseCase.search(any()))
                .willReturn(new UserDirectoryUseCase.PageResult(List.of(summary()), 1, 20, 1, 1));

        mockMvc.perform(
                        get("/api/v1/users/search")
                                .param("keyword", "hong@example.com")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].email").value("hong@example.com"));
    }

    @Test
    void searchByOrganizationFiltersSuccess() throws Exception {
        given(userDirectoryUseCase.search(any()))
                .willReturn(new UserDirectoryUseCase.PageResult(List.of(summary()), 1, 20, 1, 1));

        mockMvc.perform(
                        get("/api/v1/users/search")
                                .param("affiliateId", AFFILIATE_ID.toString())
                                .param("departmentId", DEPARTMENT_ID.toString())
                                .param("teamId", TEAM_ID.toString())
                                .param("positionId", POSITION_ID.toString())
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].affiliate").value("Affiliate"))
                .andExpect(jsonPath("$.data.items[0].department").value("Department"))
                .andExpect(jsonPath("$.data.items[0].team").value("Team"))
                .andExpect(jsonPath("$.data.items[0].position").value("Position"));
    }

    @Test
    void activeUserDefaultSearchSuccess() throws Exception {
        given(userDirectoryUseCase.search(any()))
                .willReturn(new UserDirectoryUseCase.PageResult(List.of(summary()), 1, 20, 1, 1));

        mockMvc.perform(
                        get("/api/v1/users/search")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    @Test
    void summarySuccess() throws Exception {
        given(userDirectoryUseCase.getSummary(USER_ID)).willReturn(summary());

        mockMvc.perform(
                        get("/api/v1/organization/users/{userId}/summary", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void userAndAdminCanAccess() throws Exception {
        given(userDirectoryUseCase.getSummary(USER_ID)).willReturn(summary());

        mockMvc.perform(
                        get("/api/v1/organization/users/{userId}/summary", USER_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isOk());
    }

    @Test
    void systemRoleCannotAccess() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/search")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.SYSTEM)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(userDirectoryUseCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(USER_ID, ORGANIZATION_ID, role, "User");
    }

    private UserDirectoryUseCase.UserDirectorySummary summary() {
        return new UserDirectoryUseCase.UserDirectorySummary(
                USER_ID,
                "hong",
                "Hong Gil Dong",
                "hong@example.com",
                "USER",
                "ACTIVE",
                ORGANIZATION_ID,
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "Affiliate",
                "Department",
                "Team",
                "Position");
    }
}
