package com.meetbowl.infrastructure.persistence.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.chatbot.ChatSession;
import com.meetbowl.domain.chatbot.ChatSessionRepositoryPort;
import com.meetbowl.domain.chatbot.ChatSessionStatus;

@Repository
public class JpaChatSessionRepositoryAdapter implements ChatSessionRepositoryPort {

    private final SpringDataChatSessionRepository repository;

    public JpaChatSessionRepositoryAdapter(SpringDataChatSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatSession save(ChatSession session) {
        return repository.save(ChatSessionEntity.from(session)).toDomain();
    }

    @Override
    public Optional<ChatSession> findById(UUID sessionId) {
        return repository.findById(sessionId).map(ChatSessionEntity::toDomain);
    }

    @Override
    public List<ChatSession> findActiveByOwnerUserId(UUID ownerUserId) {
        return repository
                .findByOwnerUserIdAndStatusOrderByLastMessageAtDesc(
                        ownerUserId, ChatSessionStatus.ACTIVE)
                .stream()
                .map(ChatSessionEntity::toDomain)
                .toList();
    }
}
