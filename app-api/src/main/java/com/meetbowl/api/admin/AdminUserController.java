package com.meetbowl.api.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.admin.dto.AdminResetPasswordResponse;
import com.meetbowl.api.admin.dto.AdminUserCreateRequest;
import com.meetbowl.api.admin.dto.AdminUserCreateResponse;
import com.meetbowl.api.admin.dto.AdminUserListResponse;
import com.meetbowl.api.admin.dto.AdminUserResponse;
import com.meetbowl.api.admin.dto.AdminUserSearchReindexResponse;
import com.meetbowl.api.admin.dto.AdminUserStatusUpdateRequest;
import com.meetbowl.api.admin.dto.AdminUserUpdateRequest;
import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.application.admin.AdminUserManagementUseCase;
import com.meetbowl.application.admin.AdminUserSearchIndexUseCase;
import com.meetbowl.application.admin.ResetUserPasswordCommand;
import com.meetbowl.application.admin.ResetUserPasswordResult;
import com.meetbowl.application.admin.ResetUserPasswordUseCase;
import com.meetbowl.common.response.ApiResponse;

/** 관리자 회원 계정 관리 API 컨트롤러 회원 생성, 조회, 수정, 상태 관리, 비밀번호 초기화 기능을 제공합니다. */
@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/admin/users")
public class AdminUserController extends BaseController {

    // UUID 형식의 사용자 ID 경로 변수 패턴
    private static final String USER_ID_PATH = "/{userId:[0-9a-fA-F-]{36}}";

    private final AdminUserManagementUseCase adminUserManagementUseCase;
    private final AdminUserSearchIndexUseCase adminUserSearchIndexUseCase;
    private final ResetUserPasswordUseCase resetUserPasswordUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    /**
     * AdminUserController 생성자
     *
     * @param adminUserManagementUseCase 관리자 회원 관리 유스케이스
     * @param resetUserPasswordUseCase 비밀번호 초기화 유스케이스
     * @param globalPermissionChecker 전역 권한 검사기
     */
    public AdminUserController(
            AdminUserManagementUseCase adminUserManagementUseCase,
            AdminUserSearchIndexUseCase adminUserSearchIndexUseCase,
            ResetUserPasswordUseCase resetUserPasswordUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.adminUserManagementUseCase = adminUserManagementUseCase;
        this.adminUserSearchIndexUseCase = adminUserSearchIndexUseCase;
        this.resetUserPasswordUseCase = resetUserPasswordUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    /**
     * 관리자 회원 계정 생성 API 새로운 회원 계정을 생성하고 임시 비밀번호를 발급합니다.
     *
     * @param admin 현재 로그인한 관리자 정보
     * @param request 회원 생성 요청 데이터
     * @param httpServletRequest HTTP 요청 정보 (IP, User-Agent 추출용)
     * @return 생성된 회원 정보와 임시 비밀번호
     */
    @PostMapping
    public ApiResponse<AdminUserCreateResponse> create(
            @CurrentUser AuthenticatedUser admin,
            @Valid @RequestBody AdminUserCreateRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserCreateResponse.from(
                        adminUserManagementUseCase.create(
                                new AdminUserManagementUseCase.CreateCommand(
                                        request.loginId(),
                                        request.name(),
                                        request.email(),
                                        request.role().name(),
                                        request.status().name(),
                                        request.affiliateId(),
                                        request.departmentId(),
                                        request.teamId(),
                                        request.positionId(),
                                        request.activeFrom(),
                                        request.activeUntil(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    /**
     * 관리자 회원 목록 조회 API 키워드 검색과 페이지네이션을 지원합니다.
     *
     * @param admin 현재 로그인한 관리자 정보
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @param keyword 검색 키워드 (이름, 이메일 등)
     * @return 회원 목록과 페이지 정보
     */
    @GetMapping
    public ApiResponse<AdminUserListResponse> list(
            @CurrentUser AuthenticatedUser admin,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        requireAdmin(admin);
        return ok(
                AdminUserListResponse.from(
                        adminUserManagementUseCase.search(
                                new AdminUserManagementUseCase.SearchCommand(
                                        keyword, page, size))));
    }

    @PostMapping("/search-index/reindex")
    public ApiResponse<AdminUserSearchReindexResponse> reindexSearchDocuments(
            @CurrentUser AuthenticatedUser admin) {
        requireAdmin(admin);
        return ok(AdminUserSearchReindexResponse.from(adminUserSearchIndexUseCase.reindexAll()));
    }

    /**
     * 관리자 회원 상세 조회 API 특정 회원의 상세 정보를 조회합니다.
     *
     * @param admin 현재 로그인한 관리자 정보
     * @param userId 조회할 회원 ID
     * @return 회원 상세 정보
     */
    @GetMapping(USER_ID_PATH)
    public ApiResponse<AdminUserResponse> get(
            @CurrentUser AuthenticatedUser admin, @PathVariable UUID userId) {
        requireAdmin(admin);
        return ok(AdminUserResponse.from(adminUserManagementUseCase.get(userId)));
    }

    /**
     * 관리자 회원 정보 수정 API 회원의 기본 정보(이름, 이메일, 역할, 소속 등)를 수정합니다.
     *
     * @param admin 현재 로그인한 관리자 정보
     * @param userId 수정할 회원 ID
     * @param request 회원 수정 요청 데이터
     * @param httpServletRequest HTTP 요청 정보 (IP, User-Agent 추출용)
     * @return 수정된 회원 정보
     */
    @PatchMapping(USER_ID_PATH)
    public ApiResponse<AdminUserResponse> update(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserResponse.from(
                        adminUserManagementUseCase.update(
                                new AdminUserManagementUseCase.UpdateCommand(
                                        userId,
                                        request.name(),
                                        request.email(),
                                        request.role().name(),
                                        request.affiliateId(),
                                        request.departmentId(),
                                        request.teamId(),
                                        request.positionId(),
                                        request.activeFrom(),
                                        request.activeUntil(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    /**
     * 관리자 회원 상태 수정 API 회원의 상태(ACTIVE, INACTIVE 등)를 변경합니다. 상태 변경 시 해당 회원의 모든 세션이 만료됩니다.
     *
     * @param admin 현재 로그인한 관리자 정보
     * @param userId 상태를 변경할 회원 ID
     * @param request 상태 수정 요청 데이터
     * @param httpServletRequest HTTP 요청 정보 (IP, User-Agent 추출용)
     * @return 상태가 변경된 회원 정보
     */
    @PatchMapping(USER_ID_PATH + "/status")
    public ApiResponse<AdminUserResponse> updateStatus(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserResponse.from(
                        adminUserManagementUseCase.updateStatus(
                                new AdminUserManagementUseCase.UpdateStatusCommand(
                                        userId,
                                        request.status().name(),
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    /**
     * 관리자 회원 비밀번호 초기화 API 회원의 비밀번호를 임시 비밀번호로 초기화합니다. 초기화 후 해당 회원의 모든 세션이 만료됩니다.
     *
     * @param admin 현재 로그인한 관리자 정보
     * @param userId 비밀번호를 초기화할 회원 ID
     * @param httpServletRequest HTTP 요청 정보 (IP, User-Agent 추출용)
     * @return 새로 발급된 임시 비밀번호
     */
    @DeleteMapping(USER_ID_PATH)
    public ApiResponse<AdminUserResponse> delete(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);
        return ok(
                AdminUserResponse.from(
                        adminUserManagementUseCase.delete(
                                new AdminUserManagementUseCase.DeleteCommand(
                                        userId,
                                        admin.userId(),
                                        admin.displayName(),
                                        httpServletRequest.getRemoteAddr(),
                                        httpServletRequest.getHeader("User-Agent")))));
    }

    @PostMapping(USER_ID_PATH + "/password/reset")
    public ApiResponse<AdminResetPasswordResponse> resetPassword(
            @CurrentUser AuthenticatedUser admin,
            @PathVariable UUID userId,
            HttpServletRequest httpServletRequest) {
        requireAdmin(admin);

        ResetUserPasswordResult result =
                resetUserPasswordUseCase.execute(
                        new ResetUserPasswordCommand(
                                userId,
                                admin.userId(),
                                admin.displayName(),
                                httpServletRequest.getRemoteAddr(),
                                httpServletRequest.getHeader("User-Agent")));

        return ok(new AdminResetPasswordResponse(result.temporaryPassword()));
    }

    /**
     * 관리자 권한 검사 현재 사용자가 관리자 권한을 가지고 있는지 확인합니다.
     *
     * @param admin 현재 로그인한 사용자 정보
     */
    private void requireAdmin(AuthenticatedUser admin) {
        globalPermissionChecker.requireAdmin(admin);
    }
}
