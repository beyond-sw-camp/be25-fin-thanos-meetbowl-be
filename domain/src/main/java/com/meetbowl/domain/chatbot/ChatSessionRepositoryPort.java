package com.meetbowl.domain.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 챗봇 세션 영속화를 위한 도메인 Port다.
 *
 * <p>Domain/Application은 JPA를 직접 알지 않고 이 계약에만 의존하며, Infrastructure의 JPA Adapter가 실제 MariaDB 접근을
 * 구현한다.
 */
public interface ChatSessionRepositoryPort {

    ChatSession save(ChatSession session);

    Optional<ChatSession> findById(UUID sessionId);

    List<ChatSession> findActiveByOwnerUserId(UUID ownerUserId);
}
