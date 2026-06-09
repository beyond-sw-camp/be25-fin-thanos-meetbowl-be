package com.meetbowl.domain.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepositoryPort {

    ChatMessage save(ChatMessage message);

    Optional<ChatMessage> findById(UUID messageId);

    List<ChatMessage> findBySessionId(UUID sessionId);
}
