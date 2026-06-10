package com.meetbowl.domain.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 챗봇 메시지 저장과 세션별 대화 이력 조회를 위한 도메인 Port다. */
public interface ChatMessageRepositoryPort {

    ChatMessage save(ChatMessage message);

    Optional<ChatMessage> findById(UUID messageId);

    List<ChatMessage> findBySessionId(UUID sessionId);
}
