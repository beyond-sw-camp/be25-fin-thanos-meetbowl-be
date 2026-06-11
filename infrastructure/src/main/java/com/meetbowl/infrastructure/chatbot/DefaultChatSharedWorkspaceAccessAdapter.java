package com.meetbowl.infrastructure.chatbot;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.meetbowl.domain.chatbot.ChatSharedWorkspaceAccessPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/**
 * 공유 워크스페이스 접근 권한 조회의 기본 구현이다.
 *
 * <p>활성 멤버십을 질문마다 다시 조회하고, 삭제되지 않았으며 인증 사용자의 조직에 속한 워크스페이스만 AI 검색 범위로 전달한다. 제거된 멤버십이나 다른 조직의 워크스페이스
 * ID가 과거 요청 문맥에서 재사용되지 않게 하는 권한 경계다.
 */
@Component
public class DefaultChatSharedWorkspaceAccessAdapter implements ChatSharedWorkspaceAccessPort {

    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;
    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;

    public DefaultChatSharedWorkspaceAccessAdapter(
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort,
            SharedWorkspaceRepositoryPort workspaceRepositoryPort) {
        this.memberRepositoryPort = memberRepositoryPort;
        this.workspaceRepositoryPort = workspaceRepositoryPort;
    }

    @Override
    public Set<UUID> findAccessibleSharedWorkspaceIds(UUID userId, UUID organizationId) {
        return memberRepositoryPort.findActiveByUserId(userId).stream()
                .map(member -> workspaceRepositoryPort.findById(member.workspaceId()))
                .flatMap(java.util.Optional::stream)
                .filter(workspace -> !workspace.isDeleted())
                .filter(workspace -> workspace.organizationId().equals(organizationId))
                .map(workspace -> workspace.id())
                .collect(Collectors.toUnmodifiableSet());
    }
}
