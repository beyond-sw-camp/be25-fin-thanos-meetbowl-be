package com.meetbowl.infrastructure.chatbot;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.domain.chatbot.ChatSharedWorkspaceAccessPort;

/**
 * 공유 워크스페이스 접근 권한 조회의 기본 구현이다.
 *
 * <p>현재 레포에는 공유 워크스페이스 멤버십 영속 계층이 아직 없어 빈 집합을 반환한다. 빈 집합은 "공유 자료 검색 없음"을 뜻하므로, 권한이 확인되지 않은 자료가 챗봇
 * 답변 근거로 새어 나가지 않는 안전한 기본값이다. 개인 자료 검색은 AI 서버가 인증 userId만으로 수행하므로 영향받지 않는다.
 *
 * <p>sharedworkspace 도메인이 추가되면 이 adapter를 사용자 멤버십 조회 구현으로 교체한다. 그래야 워크스페이스 권한 상실이 다음 질문부터 즉시 반영된다.
 */
@Component
public class DefaultChatSharedWorkspaceAccessAdapter implements ChatSharedWorkspaceAccessPort {

    @Override
    public Set<UUID> findAccessibleSharedWorkspaceIds(UUID userId, UUID organizationId) {
        return Set.of();
    }
}
