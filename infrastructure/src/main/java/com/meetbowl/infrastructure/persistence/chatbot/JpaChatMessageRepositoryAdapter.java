package com.meetbowl.infrastructure.persistence.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.chatbot.ChatMessage;
import com.meetbowl.domain.chatbot.ChatMessageRepositoryPort;

/** 도메인 메시지 Repository Port를 Spring Data JPA로 구현하는 영속성 Adapter다. */
@Repository
public class JpaChatMessageRepositoryAdapter implements ChatMessageRepositoryPort {

    private final SpringDataChatMessageRepository repository;

    public JpaChatMessageRepositoryAdapter(SpringDataChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        return repository.save(ChatMessageEntity.from(message)).toDomain();
    }

    @Override
    public Optional<ChatMessage> findById(UUID messageId) {
        return repository.findById(messageId).map(ChatMessageEntity::toDomain);
    }

    @Override
    public List<ChatMessage> findBySessionId(UUID sessionId) {
        return repository.findBySessionIdOrderBySequenceNumberAsc(sessionId).stream()
                .map(ChatMessageEntity::toDomain)
                .toList();
    }
}
