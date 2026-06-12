package com.meetbowl.api.sharedworkspace;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.meetbowl.api.sharedworkspace.dto.InviteSharedWorkspaceMemberRequest;
import com.meetbowl.api.sharedworkspace.dto.SharedWorkspaceMemberResponse;
import com.meetbowl.application.sharedworkspace.GetSharedWorkspaceMembersUseCase;
import com.meetbowl.application.sharedworkspace.InviteSharedWorkspaceMemberCommand;
import com.meetbowl.application.sharedworkspace.InviteSharedWorkspaceMemberUseCase;
import com.meetbowl.application.sharedworkspace.RemoveSharedWorkspaceMemberUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 공유 워크스페이스 멤버 초대/조회/추방·탈퇴 API다. 초대는 소유자만, 추방은 소유자만, 탈퇴는 본인만 가능한 권한 분기는 UseCase에서 path의 대상 사용자와 인증
 * 사용자를 비교해 판정한다.
 */
@RequireUserOrAdmin
@RestController
@RequestMapping(ApiPaths.API_V1 + "/shared-workspaces/{spaceId}/members")
public class SharedWorkspaceMemberController extends BaseController {

    private final InviteSharedWorkspaceMemberUseCase inviteSharedWorkspaceMemberUseCase;
    private final GetSharedWorkspaceMembersUseCase getSharedWorkspaceMembersUseCase;
    private final RemoveSharedWorkspaceMemberUseCase removeSharedWorkspaceMemberUseCase;

    public SharedWorkspaceMemberController(
            InviteSharedWorkspaceMemberUseCase inviteSharedWorkspaceMemberUseCase,
            GetSharedWorkspaceMembersUseCase getSharedWorkspaceMembersUseCase,
            RemoveSharedWorkspaceMemberUseCase removeSharedWorkspaceMemberUseCase) {
        this.inviteSharedWorkspaceMemberUseCase = inviteSharedWorkspaceMemberUseCase;
        this.getSharedWorkspaceMembersUseCase = getSharedWorkspaceMembersUseCase;
        this.removeSharedWorkspaceMemberUseCase = removeSharedWorkspaceMemberUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SharedWorkspaceMemberResponse>> inviteMember(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @Valid @RequestBody InviteSharedWorkspaceMemberRequest request) {
        SharedWorkspaceMemberResponse response =
                SharedWorkspaceMemberResponse.from(
                        inviteSharedWorkspaceMemberUseCase.execute(
                                new InviteSharedWorkspaceMemberCommand(
                                        spaceId, user.userId(), request.userId())));
        return created(response);
    }

    @GetMapping
    public ApiResponse<List<SharedWorkspaceMemberResponse>> getMembers(
            @CurrentUser AuthenticatedUser user, @PathVariable UUID spaceId) {
        List<SharedWorkspaceMemberResponse> members =
                getSharedWorkspaceMembersUseCase
                        .execute(spaceId, user.userId(), user.organizationId())
                        .stream()
                        .map(SharedWorkspaceMemberResponse::from)
                        .toList();
        return ok(members);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID spaceId,
            @PathVariable UUID userId) {
        removeSharedWorkspaceMemberUseCase.execute(spaceId, user.userId(), userId);
        return ok();
    }
}
