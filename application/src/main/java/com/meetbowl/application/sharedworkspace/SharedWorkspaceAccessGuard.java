package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/**
 * 공유 워크스페이스 API의 권한 판정을 한 곳으로 모은다. 권한은 화면 제어가 아니라 매 요청 서버에서 다시 계산해야 하므로, 각 UseCase가 개별적으로 멤버십을 조회하다
 * 규칙이 어긋나는 일을 막기 위해 공통 진입점으로 강제한다. 삭제된 워크스페이스는 존재를 노출하지 않도록 404로 통일한다.
 */
@Component
public class SharedWorkspaceAccessGuard {

    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;
    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;

    public SharedWorkspaceAccessGuard(
            SharedWorkspaceRepositoryPort workspaceRepositoryPort,
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort) {
        this.workspaceRepositoryPort = workspaceRepositoryPort;
        this.memberRepositoryPort = memberRepositoryPort;
    }

    /** 삭제 여부와 무관하게 식별만 필요한 경우에도, 외부에는 삭제된 워크스페이스를 없는 것으로 취급해 노출하지 않는다. */
    public SharedWorkspace requireActiveWorkspace(UUID workspaceId) {
        SharedWorkspace workspace =
                workspaceRepositoryPort
                        .findById(workspaceId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.SHARED_WORKSPACE_NOT_FOUND));
        if (workspace.isDeleted()) {
            throw new BusinessException(ErrorCode.SHARED_WORKSPACE_NOT_FOUND);
        }
        return workspace;
    }

    /**
     * 조회 권한은 활성 멤버이거나, 워크스페이스가 전 직원 공개(ORGANIZATION)이며 동일 조직 소속인 경우에 허용한다. 전 직원 공개 범위를 멤버십 없이도 읽을 수
     * 있게 허용하는 것이 핵심 요구사항이라 멤버 검사와 분리한다.
     */
    public SharedWorkspace requireReadable(UUID workspaceId, UUID userId, UUID organizationId) {
        SharedWorkspace workspace = requireActiveWorkspace(workspaceId);
        if (isActiveMember(workspaceId, userId)) {
            return workspace;
        }
        if (workspace.isOrganizationVisible()
                && workspace.organizationId().equals(organizationId)) {
            return workspace;
        }
        throw new BusinessException(ErrorCode.SHARED_WORKSPACE_FORBIDDEN);
    }

    /** 자료 업로드/버전 변경 등 멤버 권한이 필요한 작업의 공통 검증이다. 소유자도 OWNER 역할의 활성 멤버 행을 가지므로 함께 통과한다. */
    public SharedWorkspaceMember requireActiveMember(UUID workspaceId, UUID userId) {
        requireActiveWorkspace(workspaceId);
        return memberRepositoryPort
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .filter(SharedWorkspaceMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARED_WORKSPACE_FORBIDDEN));
    }

    /** 워크스페이스 정보 수정·삭제·멤버 초대처럼 소유자만 허용되는 작업에 사용한다. */
    public SharedWorkspace requireOwner(UUID workspaceId, UUID userId) {
        SharedWorkspace workspace = requireActiveWorkspace(workspaceId);
        if (!workspace.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.SHARED_WORKSPACE_FORBIDDEN);
        }
        return workspace;
    }

    public boolean isActiveMember(UUID workspaceId, UUID userId) {
        return memberRepositoryPort
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(SharedWorkspaceMember::isActive)
                .orElse(false);
    }
}
