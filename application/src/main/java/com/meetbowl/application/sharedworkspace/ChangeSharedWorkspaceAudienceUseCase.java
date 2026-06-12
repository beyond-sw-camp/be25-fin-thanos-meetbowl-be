package com.meetbowl.application.sharedworkspace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/**
 * 전 직원 공유 대상 여부를 변경한다. 기본 공개 범위는 멤버 전용이며, 전 직원 공개 전환은 자료를 조직 전체에 노출하는 결정이라 소유자 또는 관리자만 허용한다. 그래서 멤버
 * 권한 검증과 분리해 별도 권한 판정을 둔다.
 */
@Service
public class ChangeSharedWorkspaceAudienceUseCase {

    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;

    public ChangeSharedWorkspaceAudienceUseCase(
            SharedWorkspaceRepositoryPort workspaceRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard) {
        this.workspaceRepositoryPort = workspaceRepositoryPort;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public SharedWorkspaceResult execute(ChangeSharedWorkspaceAudienceCommand command) {
        SharedWorkspace workspace = accessGuard.requireActiveWorkspace(command.workspaceId());
        if (!workspace.isOwnedBy(command.requesterUserId()) && !command.requesterIsAdmin()) {
            throw new BusinessException(ErrorCode.SHARED_WORKSPACE_FORBIDDEN);
        }

        SharedWorkspace changed =
                command.openToOrganization()
                        ? workspace.openToOrganization()
                        : workspace.restrictToMembers();
        return SharedWorkspaceResult.from(workspaceRepositoryPort.save(changed));
    }
}
