package com.meetbowl.infrastructure.persistence.chatbot;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.chatbot.ChatScopeType;
import com.meetbowl.domain.chatbot.ChatSession;
import com.meetbowl.domain.chatbot.ChatSessionStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "chat_sessions",
        indexes = {
            @Index(name = "idx_chat_session_owner_status", columnList = "owner_user_id,status"),
            @Index(name = "idx_chat_session_scope", columnList = "scope_type,scope_id")
        })
public class ChatSessionEntity extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChatScopeType scopeType;

    @Column(columnDefinition = "BINARY(16)")
    private UUID scopeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatSessionStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant lastMessageAt;

    private Instant deletedAt;

    protected ChatSessionEntity() {}

    private ChatSessionEntity(
            UUID ownerUserId,
            String title,
            ChatScopeType scopeType,
            UUID scopeId,
            ChatSessionStatus status,
            Instant startedAt,
            Instant lastMessageAt,
            Instant deletedAt) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.status = status;
        this.startedAt = startedAt;
        this.lastMessageAt = lastMessageAt;
        this.deletedAt = deletedAt;
    }

    static ChatSessionEntity from(ChatSession session) {
        ChatSessionEntity entity =
                new ChatSessionEntity(
                        session.ownerUserId(),
                        session.title(),
                        session.scopeType(),
                        session.scopeId(),
                        session.status(),
                        session.startedAt(),
                        session.lastMessageAt(),
                        session.deletedAt());
        entity.setId(session.id());
        return entity;
    }

    ChatSession toDomain() {
        return ChatSession.of(
                getId(),
                ownerUserId,
                title,
                scopeType,
                scopeId,
                status,
                startedAt,
                lastMessageAt,
                deletedAt);
    }
}
