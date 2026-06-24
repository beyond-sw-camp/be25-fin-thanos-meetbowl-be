package com.meetbowl.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;
import com.meetbowl.application.auth.AccessTokenValidationService;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

@WebMvcTest(AdminOrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    CurrentUserArgumentResolver.class,
    GlobalExceptionHandler.class,
    WebMvcConfig.class,
    GlobalPermissionChecker.class
})
class AdminOrganizationControllerTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ORGANIZATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
    private static final Instant NOW = Instant.parse("2026-06-12T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminOrganizationMasterDataUseCase useCase;
    @MockitoBean private AccessTokenValidationService accessTokenValidationService;

    @Test
    void getAffiliatesSuccess() throws Exception {
        given(useCase.getAffiliates()).willReturn(List.of(affiliateResult("ACTIVE")));

        mockMvc.perform(
                        get("/api/v1/admin/organizations/affiliates")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].affiliateId").value(AFFILIATE_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    @Test
    void createAffiliateSuccess() throws Exception {
        given(useCase.createAffiliate(any())).willReturn(affiliateResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/affiliates")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Platform",
                                          "code": "PLT",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affiliateId").value(AFFILIATE_ID.toString()));
    }

    @Test
    void updateAffiliateSuccess() throws Exception {
        given(useCase.updateAffiliate(any())).willReturn(affiliateResult("ACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/admin/organizations/affiliates/{affiliateId}", AFFILIATE_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Platform Lab",
                                          "code": "PLAB",
                                          "status": "ACTIVE",
                                          "sortOrder": 2
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("PLT"));
    }

    @Test
    void updateAffiliateStatusSuccess() throws Exception {
        given(useCase.updateAffiliateStatus(any())).willReturn(affiliateResult("INACTIVE"));

        mockMvc.perform(
                        patch(
                                        "/api/v1/admin/organizations/affiliates/{affiliateId}/status",
                                        AFFILIATE_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void getDepartmentsSuccess() throws Exception {
        given(useCase.getDepartments()).willReturn(List.of(departmentResult("ACTIVE")));

        mockMvc.perform(
                        get("/api/v1/admin/organizations/departments")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.items[0].departmentId").value(DEPARTMENT_ID.toString()));
    }

    @Test
    void createDepartmentSuccess() throws Exception {
        given(useCase.createDepartment(any())).willReturn(departmentResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/departments")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "affiliateId": "%s",
                                          "name": "Engineering",
                                          "code": "ENG",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """
                                                .formatted(AFFILIATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affiliateId").value(AFFILIATE_ID.toString()));
    }

    @Test
    void createDepartmentSucceedsWithoutCodeField() throws Exception {
        given(useCase.createDepartment(any())).willReturn(departmentResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/departments")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "affiliateId": "%s",
                                          "name": "Engineering",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """
                                                .formatted(AFFILIATE_ID)))
                .andExpect(status().isOk());
    }

    @Test
    void createDepartmentReturnsConflictWhenSortOrderDuplicated() throws Exception {
        given(useCase.createDepartment(any()))
                .willThrow(
                        new BusinessException(
                                ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED,
                                "이미 사용 중인 순서입니다. 다른 순서를 입력해 주세요."));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/departments")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "affiliateId": "%s",
                                          "name": "Engineering",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """
                                                .formatted(AFFILIATE_ID)))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.error.code")
                                .value("ORGANIZATION_SORT_ORDER_DUPLICATED"))
                .andExpect(
                        jsonPath("$.error.message")
                                .value("이미 사용 중인 순서입니다. 다른 순서를 입력해 주세요."));
    }

    @Test
    void updateDepartmentSuccess() throws Exception {
        given(useCase.updateDepartment(any())).willReturn(departmentResult("ACTIVE"));

        mockMvc.perform(
                        patch(
                                        "/api/v1/admin/organizations/departments/{departmentId}",
                                        DEPARTMENT_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "affiliateId": "%s",
                                          "name": "Core Engineering",
                                          "code": "CENG",
                                          "status": "ACTIVE",
                                          "sortOrder": 2
                                        }
                                        """
                                                .formatted(AFFILIATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(DEPARTMENT_ID.toString()));
    }

    @Test
    void updateDepartmentStatusSuccess() throws Exception {
        given(useCase.updateDepartmentStatus(any())).willReturn(departmentResult("INACTIVE"));

        mockMvc.perform(
                        patch(
                                        "/api/v1/admin/organizations/departments/{departmentId}/status",
                                        DEPARTMENT_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void deleteDepartmentSuccess() throws Exception {
        mockMvc.perform(
                        delete(
                                        "/api/v1/admin/organizations/departments/{departmentId}",
                                        DEPARTMENT_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminOrganizationControllerTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getTeamsSuccess() throws Exception {
        given(useCase.getTeams()).willReturn(List.of(teamResult("ACTIVE")));

        mockMvc.perform(
                        get("/api/v1/admin/organizations/teams")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].teamId").value(TEAM_ID.toString()));
    }

    @Test
    void createTeamSuccess() throws Exception {
        given(useCase.createTeam(any())).willReturn(teamResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/teams")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "departmentId": "%s",
                                          "name": "Backend",
                                          "code": "BE",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """
                                                .formatted(DEPARTMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(DEPARTMENT_ID.toString()));
    }

    @Test
    void createTeamSucceedsWithoutCodeField() throws Exception {
        given(useCase.createTeam(any())).willReturn(teamResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/teams")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "departmentId": "%s",
                                          "name": "Backend",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """
                                                .formatted(DEPARTMENT_ID)))
                .andExpect(status().isOk());
    }

    @Test
    void updateTeamSuccess() throws Exception {
        given(useCase.updateTeam(any())).willReturn(teamResult("ACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/admin/organizations/teams/{teamId}", TEAM_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "departmentId": "%s",
                                          "name": "Platform Backend",
                                          "code": "PBE",
                                          "status": "ACTIVE",
                                          "sortOrder": 2
                                        }
                                        """
                                                .formatted(DEPARTMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamId").value(TEAM_ID.toString()));
    }

    @Test
    void updateTeamStatusSuccess() throws Exception {
        given(useCase.updateTeamStatus(any())).willReturn(teamResult("INACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/admin/organizations/teams/{teamId}/status", TEAM_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void deleteTeamSuccess() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/admin/organizations/teams/{teamId}", TEAM_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminOrganizationControllerTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPositionsSuccess() throws Exception {
        given(useCase.getPositions()).willReturn(List.of(positionResult("ACTIVE")));

        mockMvc.perform(
                        get("/api/v1/admin/organizations/positions")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].positionId").value(POSITION_ID.toString()));
    }

    @Test
    void createPositionSuccess() throws Exception {
        given(useCase.createPosition(any())).willReturn(positionResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/positions")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Manager",
                                          "code": "MGR",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionId").value(POSITION_ID.toString()));
    }

    @Test
    void createPositionSucceedsWithoutCodeField() throws Exception {
        given(useCase.createPosition(any())).willReturn(positionResult("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/admin/organizations/positions")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Manager",
                                          "status": "ACTIVE",
                                          "sortOrder": 1
                                        }
                                        """))
                .andExpect(status().isOk());
    }

    @Test
    void updatePositionSuccess() throws Exception {
        given(useCase.updatePosition(any())).willReturn(positionResult("ACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/admin/organizations/positions/{positionId}", POSITION_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Senior Manager",
                                          "code": "SMGR",
                                          "status": "ACTIVE",
                                          "sortOrder": 2
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionId").value(POSITION_ID.toString()));
    }

    @Test
    void updatePositionStatusSuccess() throws Exception {
        given(useCase.updatePositionStatus(any())).willReturn(positionResult("INACTIVE"));

        mockMvc.perform(
                        patch(
                                        "/api/v1/admin/organizations/positions/{positionId}/status",
                                        POSITION_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void deletePositionSuccess() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/admin/organizations/positions/{positionId}", POSITION_ID)
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.ADMIN))
                                .header("User-Agent", "AdminOrganizationControllerTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void userRoleCannotAccessAdminOrganizationApis() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/organizations/affiliates")
                                .requestAttr(
                                        AuthenticatedUserAttributes.CURRENT_USER,
                                        authenticatedUser(AuthenticatedUserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_FORBIDDEN"));

        verifyNoInteractions(useCase);
    }

    private AuthenticatedUser authenticatedUser(AuthenticatedUserRole role) {
        return new AuthenticatedUser(ADMIN_ID, ORGANIZATION_ID, role, "Admin");
    }

    private AdminOrganizationMasterDataUseCase.AffiliateResult affiliateResult(String status) {
        return new AdminOrganizationMasterDataUseCase.AffiliateResult(
                AFFILIATE_ID, "Platform", "PLT", status, 1, NOW, NOW);
    }

    private AdminOrganizationMasterDataUseCase.DepartmentResult departmentResult(String status) {
        return new AdminOrganizationMasterDataUseCase.DepartmentResult(
                DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG", status, 1, NOW, NOW);
    }

    private AdminOrganizationMasterDataUseCase.TeamResult teamResult(String status) {
        return new AdminOrganizationMasterDataUseCase.TeamResult(
                TEAM_ID, DEPARTMENT_ID, "Backend", "BE", status, 1, NOW, NOW);
    }

    private AdminOrganizationMasterDataUseCase.PositionResult positionResult(String status) {
        return new AdminOrganizationMasterDataUseCase.PositionResult(
                POSITION_ID, "Manager", "MGR", status, 1, NOW, NOW);
    }
}
