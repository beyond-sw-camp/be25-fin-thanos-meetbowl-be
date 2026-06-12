package com.meetbowl.api.user;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.GlobalPermissionChecker;
import com.meetbowl.api.user.dto.UserDirectorySearchRequest;
import com.meetbowl.api.user.dto.UserDirectorySummaryResponse;
import com.meetbowl.application.user.UserDirectoryUseCase;
import com.meetbowl.common.response.ApiResponse;
import com.meetbowl.common.response.PageResponse;

@RestController
@RequestMapping(ApiPaths.API_V1)
public class UserDirectoryController extends BaseController {

    private final UserDirectoryUseCase userDirectoryUseCase;
    private final GlobalPermissionChecker globalPermissionChecker;

    public UserDirectoryController(
            UserDirectoryUseCase userDirectoryUseCase,
            GlobalPermissionChecker globalPermissionChecker) {
        this.userDirectoryUseCase = userDirectoryUseCase;
        this.globalPermissionChecker = globalPermissionChecker;
    }

    @GetMapping("/users/search")
    public ApiResponse<PageResponse<UserDirectorySummaryResponse>> search(
            @CurrentUser AuthenticatedUser currentUser, UserDirectorySearchRequest request) {
        // 메일 수신자 선택, 조직도 검색 모두 USER/ADMIN 공용 기능이다.
        requireUserOrAdmin(currentUser);
        UserDirectoryUseCase.PageResult result =
                userDirectoryUseCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                request.keyword(),
                                request.affiliateId(),
                                request.departmentId(),
                                request.teamId(),
                                request.positionId(),
                                request.status(),
                                request.page(),
                                request.size()));
        return ok(
                new PageResponse<>(
                        UserDirectorySummaryResponse.from(result.items()),
                        result.page(),
                        result.size(),
                        result.totalElements(),
                        result.totalPages()));
    }

    @GetMapping("/organization/users/{userId:[0-9a-fA-F-]{36}}/summary")
    public ApiResponse<UserDirectorySummaryResponse> getSummary(
            @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID userId) {
        // userId는 UUID 형식만 허용해서 잘못된 문자열이 서비스 계층까지 내려가지 않게 한다.
        requireUserOrAdmin(currentUser);
        return ok(UserDirectorySummaryResponse.from(userDirectoryUseCase.getSummary(userId)));
    }

    private void requireUserOrAdmin(AuthenticatedUser currentUser) {
        // 기존 사용자 API와 동일한 권한 진입점으로 맞춰 403 처리 기준을 통일한다.
        globalPermissionChecker.requireUserOrAdmin(currentUser);
    }
}
