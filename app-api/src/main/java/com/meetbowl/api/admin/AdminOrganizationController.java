package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminAffiliateListResponse;
import com.meetbowl.api.admin.dto.AdminAffiliateRequest;
import com.meetbowl.api.admin.dto.AdminAffiliateResponse;
import com.meetbowl.api.admin.dto.AdminDepartmentListResponse;
import com.meetbowl.api.admin.dto.AdminDepartmentRequest;
import com.meetbowl.api.admin.dto.AdminDepartmentResponse;
import com.meetbowl.api.admin.dto.AdminPositionListResponse;
import com.meetbowl.api.admin.dto.AdminPositionRequest;
import com.meetbowl.api.admin.dto.AdminPositionResponse;
import com.meetbowl.api.admin.dto.AdminReferenceStatusUpdateRequest;
import com.meetbowl.api.admin.dto.AdminTeamListResponse;
import com.meetbowl.api.admin.dto.AdminTeamRequest;
import com.meetbowl.api.admin.dto.AdminTeamResponse;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminOrganizationMasterDataUseCase;
import com.meetbowl.common.response.ApiResponse;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/organizations")
public class AdminOrganizationController extends BaseController {

    // UUID 형식만 경로 변수로 허용해 잘못된 ID 문자열이 컨트롤러까지 들어오는 것을 줄인다.
    private static final String AFFILIATE_ID_PATH = "/affiliates/{affiliateId:[0-9a-fA-F-]{36}}";
    private static final String DEPARTMENT_ID_PATH = "/departments/{departmentId:[0-9a-fA-F-]{36}}";
    private static final String TEAM_ID_PATH = "/teams/{teamId:[0-9a-fA-F-]{36}}";
    private static final String POSITION_ID_PATH = "/positions/{positionId:[0-9a-fA-F-]{36}}";

    private final AdminOrganizationMasterDataUseCase useCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public AdminOrganizationController(
            AdminOrganizationMasterDataUseCase useCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.useCase = useCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping("/affiliates")
    public ApiResponse<AdminAffiliateListResponse> getAffiliates(
            @CurrentUser AuthenticatedUser admin) {
        // 조직 기준정보 관리 API는 전부 관리자 전용이다.
        requireAdmin(admin);
        return ok(AdminAffiliateListResponse.from(useCase.getAffiliates()));
    }

    @PostMapping("/affiliates")
    public ApiResponse<AdminAffiliateResponse> createAffiliate(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody AdminAffiliateRequest request) {
        requireAdmin(admin);
        return ok(
                AdminAffiliateResponse.from(
                        useCase.createAffiliate(
                                new AdminOrganizationMasterDataUseCase.CreateAffiliateCommand(
                                        request.name(),
                                        request.code(),
                                        request.status().name(),
                                        request.sortOrder()))));
    }

    @PatchMapping(AFFILIATE_ID_PATH)
    public ApiResponse<AdminAffiliateResponse> updateAffiliate(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID affiliateId,
            @Valid @RequestBody AdminAffiliateRequest request) {
        requireAdmin(admin);
        return ok(
                AdminAffiliateResponse.from(
                        useCase.updateAffiliate(
                                new AdminOrganizationMasterDataUseCase.UpdateAffiliateCommand(
                                        affiliateId,
                                        request.name(),
                                        request.code(),
                                        request.sortOrder(),
                                        admin.userId()))));
    }

    @PatchMapping(AFFILIATE_ID_PATH + "/status")
    public ApiResponse<AdminAffiliateResponse> updateAffiliateStatus(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID affiliateId,
            @Valid @RequestBody AdminReferenceStatusUpdateRequest request) {
        requireAdmin(admin);
        return ok(
                AdminAffiliateResponse.from(
                        useCase.updateAffiliateStatus(
                                new AdminOrganizationMasterDataUseCase.UpdateAffiliateStatusCommand(
                                        affiliateId, request.status().name()))));
    }

    @GetMapping("/departments")
    public ApiResponse<AdminDepartmentListResponse> getDepartments(
            @CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(AdminDepartmentListResponse.from(useCase.getDepartments()));
    }

    @PostMapping("/departments")
    public ApiResponse<AdminDepartmentResponse> createDepartment(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody AdminDepartmentRequest request) {
        requireAdmin(admin);
        return ok(
                AdminDepartmentResponse.from(
                        useCase.createDepartment(
                                new AdminOrganizationMasterDataUseCase.CreateDepartmentCommand(
                                        request.affiliateId(),
                                        request.name(),
                                        request.code(),
                                        request.status().name(),
                                        request.sortOrder()))));
    }

    @PatchMapping(DEPARTMENT_ID_PATH)
    public ApiResponse<AdminDepartmentResponse> updateDepartment(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID departmentId,
            @Valid @RequestBody AdminDepartmentRequest request) {
        requireAdmin(admin);
        return ok(
                AdminDepartmentResponse.from(
                        useCase.updateDepartment(
                                new AdminOrganizationMasterDataUseCase.UpdateDepartmentCommand(
                                        departmentId,
                                        request.affiliateId(),
                                        request.name(),
                                        request.code(),
                                        request.sortOrder(),
                                        admin.userId()))));
    }

    @PatchMapping(DEPARTMENT_ID_PATH + "/status")
    public ApiResponse<AdminDepartmentResponse> updateDepartmentStatus(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID departmentId,
            @Valid @RequestBody AdminReferenceStatusUpdateRequest request) {
        requireAdmin(admin);
        return ok(
                AdminDepartmentResponse.from(
                        useCase.updateDepartmentStatus(
                                new AdminOrganizationMasterDataUseCase
                                        .UpdateDepartmentStatusCommand(
                                        departmentId, request.status().name()))));
    }

    @GetMapping("/teams")
    public ApiResponse<AdminTeamListResponse> getTeams(@CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(AdminTeamListResponse.from(useCase.getTeams()));
    }

    @PostMapping("/teams")
    public ApiResponse<AdminTeamResponse> createTeam(
            @CurrentUser AuthenticatedUser admin, @Valid @RequestBody AdminTeamRequest request) {
        requireAdmin(admin);
        return ok(
                AdminTeamResponse.from(
                        useCase.createTeam(
                                new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                        request.departmentId(),
                                        request.name(),
                                        request.code(),
                                        request.status().name(),
                                        request.sortOrder()))));
    }

    @PatchMapping(TEAM_ID_PATH)
    public ApiResponse<AdminTeamResponse> updateTeam(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID teamId,
            @Valid @RequestBody AdminTeamRequest request) {
        requireAdmin(admin);
        return ok(
                AdminTeamResponse.from(
                        useCase.updateTeam(
                                new AdminOrganizationMasterDataUseCase.UpdateTeamCommand(
                                        teamId,
                                        request.departmentId(),
                                        request.name(),
                                        request.code(),
                                        request.sortOrder(),
                                        admin.userId()))));
    }

    @PatchMapping(TEAM_ID_PATH + "/status")
    public ApiResponse<AdminTeamResponse> updateTeamStatus(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID teamId,
            @Valid @RequestBody AdminReferenceStatusUpdateRequest request) {
        requireAdmin(admin);
        return ok(
                AdminTeamResponse.from(
                        useCase.updateTeamStatus(
                                new AdminOrganizationMasterDataUseCase.UpdateTeamStatusCommand(
                                        teamId, request.status().name()))));
    }

    @GetMapping("/positions")
    public ApiResponse<AdminPositionListResponse> getPositions(
            @CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(AdminPositionListResponse.from(useCase.getPositions()));
    }

    @PostMapping("/positions")
    public ApiResponse<AdminPositionResponse> createPosition(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody AdminPositionRequest request) {
        requireAdmin(admin);
        return ok(
                AdminPositionResponse.from(
                        useCase.createPosition(
                                new AdminOrganizationMasterDataUseCase.CreatePositionCommand(
                                        request.name(),
                                        request.code(),
                                        request.status().name(),
                                        request.sortOrder()))));
    }

    @PatchMapping(POSITION_ID_PATH)
    public ApiResponse<AdminPositionResponse> updatePosition(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID positionId,
            @Valid @RequestBody AdminPositionRequest request) {
        requireAdmin(admin);
        return ok(
                AdminPositionResponse.from(
                        useCase.updatePosition(
                                new AdminOrganizationMasterDataUseCase.UpdatePositionCommand(
                                        positionId,
                                        request.name(),
                                        request.code(),
                                        request.sortOrder(),
                                        admin.userId()))));
    }

    @PatchMapping(POSITION_ID_PATH + "/status")
    public ApiResponse<AdminPositionResponse> updatePositionStatus(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID positionId,
            @Valid @RequestBody AdminReferenceStatusUpdateRequest request) {
        requireAdmin(admin);
        return ok(
                AdminPositionResponse.from(
                        useCase.updatePositionStatus(
                                new AdminOrganizationMasterDataUseCase.UpdatePositionStatusCommand(
                                        positionId, request.status().name()))));
    }

    private void requireAdmin(AuthenticatedUser admin) {
        // 기존 관리자 권한 정책과 동일한 진입점으로 맞춰 403 처리 기준을 통일한다.
        globalPermissionChecker.requireAdmin(admin);
    }
}
