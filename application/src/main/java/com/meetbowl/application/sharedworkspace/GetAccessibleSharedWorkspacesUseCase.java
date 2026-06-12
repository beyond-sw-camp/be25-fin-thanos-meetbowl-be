package com.meetbowl.application.sharedworkspace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/**
 * 사용자가 현재 접근 가능한 공유 워크스페이스를 모은다. 접근 경로는 두 가지로, 활성 멤버로 속한 워크스페이스와 동일 조직의 전 직원 공개 워크스페이스다. 두 경로가 겹칠 수
 * 있어 워크스페이스 ID로 중복을 제거한다. 멤버십이 바뀌면 다음 조회부터 즉시 반영되도록 매번 다시 계산한다.
 */
@Service
public class GetAccessibleSharedWorkspacesUseCase {

    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;
    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;

    public GetAccessibleSharedWorkspacesUseCase(
            SharedWorkspaceRepositoryPort workspaceRepositoryPort,
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort) {
        this.workspaceRepositoryPort = workspaceRepositoryPort;
        this.memberRepositoryPort = memberRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<SharedWorkspaceResult> execute(UUID userId, UUID organizationId) {
        Map<UUID, SharedWorkspace> accessible = new LinkedHashMap<>();

        for (SharedWorkspaceMember membership : memberRepositoryPort.findActiveByUserId(userId)) {
            workspaceRepositoryPort
                    .findById(membership.workspaceId())
                    .filter(workspace -> !workspace.isDeleted())
                    .ifPresent(workspace -> accessible.putIfAbsent(workspace.id(), workspace));
        }

        for (SharedWorkspace workspace :
                workspaceRepositoryPort.findOrganizationVisible(organizationId)) {
            if (!workspace.isDeleted()) {
                accessible.putIfAbsent(workspace.id(), workspace);
            }
        }

        return accessible.values().stream().map(SharedWorkspaceResult::from).toList();
    }
}
