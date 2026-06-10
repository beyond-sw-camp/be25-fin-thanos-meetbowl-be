package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/**
 * 공유 워크스페이스에 멤버를 초대한다. 소유자만 초대할 수 있다.
 *
 * <p>재직 상태는 사용자 도메인을 기준으로 확인한다. 추방되거나 자진 탈퇴해 비활성 상태로 남은 멤버 행은 재활성화로 다시 초대하지만, 퇴사(비활성 계정)한 사용자는 새로
 * 초대하거나 재초대할 수 없다. 이미 활성 멤버이면 도메인 reactivate가 충돌로 막으므로, 같은 사용자를 중복으로 초대해도 멤버 행이 늘어나지 않는다.
 */
@Service
public class InviteSharedWorkspaceMemberUseCase {

    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final Clock clock;

    public InviteSharedWorkspaceMemberUseCase(
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            Clock clock) {
        this.memberRepositoryPort = memberRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.accessGuard = accessGuard;
        this.clock = clock;
    }

    @Transactional
    public SharedWorkspaceMemberResult execute(InviteSharedWorkspaceMemberCommand command) {
        accessGuard.requireOwner(command.workspaceId(), command.inviterUserId());

        User invitee =
                userRepositoryPort
                        .findById(command.inviteeUserId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 비활성 계정은 퇴사자로 간주해 초대 대상에서 제외한다. 권한이 남은 멤버 행을 재활성화하는 것과 구분하기 위해 사용자 상태를 먼저 본다.
        if (invitee.isInactive() || invitee.isLocked()) {
            throw new BusinessException(ErrorCode.SHARED_WORKSPACE_MEMBER_RESIGNED);
        }

        Instant now = Instant.now(clock);
        Optional<SharedWorkspaceMember> existing =
                memberRepositoryPort.findByWorkspaceIdAndUserId(
                        command.workspaceId(), command.inviteeUserId());

        SharedWorkspaceMember member =
                existing.map(found -> found.reactivate(command.inviterUserId(), now))
                        .orElseGet(
                                () ->
                                        SharedWorkspaceMember.invite(
                                                command.workspaceId(),
                                                command.inviteeUserId(),
                                                command.inviterUserId(),
                                                now));
        return SharedWorkspaceMemberResult.from(memberRepositoryPort.save(member));
    }
}
