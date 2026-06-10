package com.meetbowl.infrastructure.persistence.chatbot;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.chatbot.ChatMessageCitation;
import com.meetbowl.domain.chatbot.ChatMessageCitationRepositoryPort;

/** 도메인 citation Repository Port를 Spring Data JPA로 구현하는 영속성 Adapter다. */
@Repository
public class JpaChatMessageCitationRepositoryAdapter implements ChatMessageCitationRepositoryPort {

    private final SpringDataChatMessageCitationRepository repository;

    public JpaChatMessageCitationRepositoryAdapter(
            SpringDataChatMessageCitationRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatMessageCitation save(ChatMessageCitation citation) {
        return repository.save(ChatMessageCitationEntity.from(citation)).toDomain();
    }

    @Override
    public List<ChatMessageCitation> findByMessageId(UUID messageId) {
        return repository.findByMessageIdOrderByDisplayOrderAsc(messageId).stream()
                .map(ChatMessageCitationEntity::toDomain)
                .toList();
    }
}
