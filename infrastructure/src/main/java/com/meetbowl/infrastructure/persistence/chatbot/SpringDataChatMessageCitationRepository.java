package com.meetbowl.infrastructure.persistence.chatbot;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataChatMessageCitationRepository
        extends JpaRepository<ChatMessageCitationEntity, UUID> {

    List<ChatMessageCitationEntity> findByMessageIdOrderByDisplayOrderAsc(UUID messageId);
}
