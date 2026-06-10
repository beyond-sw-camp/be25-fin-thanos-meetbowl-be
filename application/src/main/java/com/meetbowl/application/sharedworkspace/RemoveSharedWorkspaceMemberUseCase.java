package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;

/**
 * 멤버를 추방하거나 본인이 탈퇴한다. 추방은 소유자만, 탈퇴는 본인만 가능하므로 두 경로를 한 곳에서 권한 판정한다. 소유자는 워크스페이스의 단일 책임자라 제거 대상이 될 수
 * 없고, 이 불변식은 도메인 remove에서 다시 막는다.
 */
@Service
public class RemoveSharedWorkspaceMemberUseCase {

    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final Clock clock;

    public RemoveSharedWorkspaceMemberUseCase(
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            Clock clock) {
        this.memberRepositoryPort = memberRepositoryPort;
        this.accessGuard = accessGuard;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID workspaceId, UUID requesterUserId, UUID targetUserId) {
        SharedWorkspace workspace = accessGuard.requireActiveWorkspace(workspaceId);

        boolean selfLeave = requesterUserId.equals(targetUserId);
        if (!selfLeave && !workspace.isOwnedBy(requesterUserId)) {
            throw new BusinessException(ErrorCode.SHARED_WORKSPACE_FORBIDDEN);
        }

        SharedWorkspaceMember member =
                memberRepositoryPort
                        .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                        .filter(SharedWorkspaceMember::isActive)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_MEMBER_NOT_FOUND));

        memberRepositoryPort.save(member.remove(Instant.now(clock)));
    }
}
