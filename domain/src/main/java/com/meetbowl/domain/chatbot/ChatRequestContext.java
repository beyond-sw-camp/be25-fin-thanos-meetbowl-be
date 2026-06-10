package com.meetbowl.domain.chatbot;

import java.util.Set;
import java.util.UUID;

/**
 * 한 번의 챗봇 실행에 필요한 사용자 입력과 신뢰된 검색 권한 범위를 묶는다.
 *
 * <p>대화 세션 대신 요청 단위 객체를 사용해야 서버가 이전 대화를 보유하지 않아도 후속 질문을 처리할 수 있고, 실행이 끝난 뒤 폐기해야 할 데이터 경계도 분명해진다.
 *
 * <p>검색 권한은 인증된 userId와 현재 접근 가능한 공유 워크스페이스 목록으로만 표현한다. AI 서버는 Qdrant metadata의 ownerUserId 또는
 * sharedWorkspaceId로만 필터링해야 하며, 모델이 이 범위를 임의로 넓힐 수 없도록 BE가 요청마다 멤버십을 다시 계산해 채운다.
 */
public record ChatRequestContext(
        String question,
        ChatConversationContext conversation,
        UUID userId,
        Set<UUID> sharedWorkspaceIds) {

    public ChatRequestContext {
        question =
                ChatDomainValidators.requireText(
                        question,
                        ChatMessage.MAX_CONTENT_LENGTH,
                        "챗봇 질문은 필수입니다.",
                        "챗봇 질문은 20000자 이하여야 합니다.");
        conversation = conversation == null ? ChatConversationContext.empty() : conversation;
        ChatDomainValidators.requireId(userId, "챗봇 요청 사용자 ID는 필수입니다.");
        sharedWorkspaceIds = sharedWorkspaceIds == null ? Set.of() : sharedWorkspaceIds;
        if (sharedWorkspaceIds.stream().anyMatch(workspaceId -> workspaceId == null)) {
            throw ChatDomainValidators.invalid("공유 워크스페이스 허용 범위에 빈 ID를 포함할 수 없습니다.");
        }
        sharedWorkspaceIds = Set.copyOf(sharedWorkspaceIds);
    }
}
