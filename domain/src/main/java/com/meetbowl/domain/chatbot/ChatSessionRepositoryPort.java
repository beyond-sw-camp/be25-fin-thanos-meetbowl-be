package com.meetbowl.domain.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepositoryPort {

    ChatSession save(ChatSession session);

    Optional<ChatSession> findById(UUID sessionId);

    List<ChatSession> findActiveByOwnerUserId(UUID ownerUserId);
}
