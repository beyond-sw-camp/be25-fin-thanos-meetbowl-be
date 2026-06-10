package com.meetbowl.infrastructure.persistence.chatbot;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meetbowl.domain.chatbot.ChatSessionStatus;

interface SpringDataChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {

    List<ChatSessionEntity> findByOwnerUserIdAndStatusOrderByLastMessageAtDesc(
            UUID ownerUserId, ChatSessionStatus status);
}
