package com.meetbowl.api.sharedworkspace;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.common.auth.RequireUserOrAdmin;
import com.meetbowl.api.sharedworkspace.dto.ChangeSharedWorkspaceAudienceRequest;
import com.meetbowl.api.sharedworkspace.dto.CreateSharedWorkspaceRequest;
import com.meetbowl.api.sharedworkspace.dto.SharedWorkspaceResponse;
import com.meetbowl.api.sharedworkspace.dto.UpdateSharedWorkspaceRequest;
import com.meetbowl.application.sharedworkspace.ChangeSharedWorkspaceAudienceCommand;
import com.meetbowl.application.sharedworkspace.ChangeSharedWorkspaceAudienceUseCase;
import com.meetbowl.application.sharedworkspace.CreateSharedWorkspaceCommand;
import com.meetbowl.application.sharedworkspace.CreateSharedWorkspaceUseCase;
import com.meetbowl.application.sharedworkspace.DeleteSharedWorkspaceUseCase;
import com.meetbowl.application.sharedworkspace.GetAccessibleSharedWorkspacesUseCase;
import com.meetbowl.application.sharedworkspace.GetSharedWorkspaceUseCase;
import com.meetbowl.application.sharedworkspace.UpdateSharedWorkspaceCommand;
import com.meetbowl.application.sharedworkspace.UpdateSharedWorkspaceUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 공유 워크스페이스 자체의 CRUD와 공개 범위 변경 API다. 인증된 일반 사용자/관리자만 진입할 수 있고, 워크스페이스 단위의 소유자/멤버 권한은 각 UseCase가 매
 * 요청 다시 판정한다. 화면 제어만으로 권한을 보장하지 않기 위해 path 식별자와 인증 사용자 ID를 항상 함께 넘긴다.
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/shared-workspaces")
public class SharedWorkspaceController extends BaseController {

    private final CreateSharedWorkspaceUseCase createSharedWorkspaceUseCase;
    private final GetAccessibleSharedWorkspacesUseCase getAccessibleSharedWorkspacesUseCase;
    private final GetSharedWorkspaceUseCase getSharedWorkspaceUseCase;
    private final UpdateSharedWorkspaceUseCase updateSharedWorkspaceUseCase;
    private final DeleteSharedWorkspaceUseCase deleteSharedWorkspaceUseCase;
    private final ChangeSharedWorkspaceAudienceUseCase changeSharedWorkspaceAudienceUseCase;

    public SharedWorkspaceController(
            CreateSharedWorkspaceUseCase createSharedWorkspaceUseCase,
            GetAccessibleSharedWorkspacesUseCase getAccessibleSharedWorkspacesUseCase,
            GetSharedWorkspaceUseCase getSharedWorkspaceUseCase,
            UpdateSharedWorkspaceUseCase updateSharedWorkspaceUseCase,
            DeleteSharedWorkspaceUseCase deleteSharedWorkspaceUseCase,
            ChangeSharedWorkspaceAudienceUseCase changeSharedWorkspaceAudienceUseCase) {
        this.createSharedWorkspaceUseCase = createSharedWorkspaceUseCase;
        this.getAccessibleSharedWorkspacesUseCase = getAccessibleSharedWorkspacesUseCase;
        this.getSharedWorkspaceUseCase = getSharedWorkspaceUseCase;
        this.updateSharedWorkspaceUseCase = updateSharedWorkspaceUseCase;
        this.deleteSharedWorkspaceUseCase = deleteSharedWorkspaceUseCase;
        this.changeSharedWorkspaceAudienceUseCase = changeSharedWorkspaceAudienceUseCase;
    }

    @GetMapping
    public ApiResponse<List<SharedWorkspaceResponse>> getAccessibleWorkspaces(
            @CurrentUser AuthenticatedUser user) {
        List<SharedWorkspaceResponse> workspaces =
                getAccessibleSharedWorkspacesUseCase
                        .execute(user.userId(), user.organizationId())
                        .stream()
                        .map(SharedWorkspaceResponse::from)
                        .toList();
        return ok(workspaces);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SharedWorkspaceResponse>> createWorkspace(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateSharedWorkspaceRequest request) {
        SharedWorkspaceResponse response =
                SharedWorkspaceResponse.from(
                        createSharedWorkspaceUseCase.execute(
                                new CreateSharedWorkspaceCommand(
                                        user.organizationId(),
                                        user.userId(),
                                        request.name(),
                                        request.description())));
        return created(response);
    }

    @GetMapping("/{spaceId}")
    public ApiResponse<SharedWorkspaceResponse> getWorkspace(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID spaceId) {
        return ok(
                SharedWorkspaceResponse.from(
                        getSharedWorkspaceUseCase.execute(
                                spaceId, user.userId(), user.organizationId())));
    }

    @PatchMapping("/{spaceId}")
    public ApiResponse<SharedWorkspaceResponse> updateWorkspace(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSharedWorkspaceRequest request) {
        return ok(
                SharedWorkspaceResponse.from(
                        updateSharedWorkspaceUseCase.execute(
                                new UpdateSharedWorkspaceCommand(
                                        spaceId,
                                        user.userId(),
                                        request.name(),
                                        request.description()))));
    }

    @DeleteMapping("/{spaceId}")
    public ApiResponse<Void> deleteWorkspace(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID spaceId) {
        deleteSharedWorkspaceUseCase.execute(spaceId, user.userId());
        return ok();
    }

    @PatchMapping("/{spaceId}/audience")
    public ApiResponse<SharedWorkspaceResponse> changeAudience(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @Valid @RequestBody ChangeSharedWorkspaceAudienceRequest request) {
        return ok(
                SharedWorkspaceResponse.from(
                        changeSharedWorkspaceAudienceUseCase.execute(
                                new ChangeSharedWorkspaceAudienceCommand(
                                        spaceId,
                                        user.userId(),
                                        user.isAdmin(),
                                        request.openToOrganization()))));
    }
}
