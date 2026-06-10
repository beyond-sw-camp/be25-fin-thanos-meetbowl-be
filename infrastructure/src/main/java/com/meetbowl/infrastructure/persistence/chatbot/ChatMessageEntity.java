package com.meetbowl.infrastructure.persistence.chatbot;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.chatbot.ChatMessage;
import com.meetbowl.domain.chatbot.ChatMessageRole;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "chat_messages",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_chat_message_session_sequence",
                        columnNames = {"session_id", "sequence_number"}),
        indexes = @Index(name = "idx_chat_message_session", columnList = "session_id"))
public class ChatMessageEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatMessageRole role;

    @Column(columnDefinition = "BINARY(16)")
    private UUID senderUserId;

    @Column(nullable = false)
    private int sequenceNumber;

    @Column(nullable = false, length = 20000)
    private String content;

    @Column(length = 100)
    private String modelName;

    @Column(length = 50)
    private String promptVersion;

    @Column(length = 100)
    private String aiRequestId;

    @Column(nullable = false)
    private Instant messageCreatedAt;

    protected ChatMessageEntity() {}

    private ChatMessageEntity(
            UUID sessionId,
            ChatMessageRole role,
            UUID senderUserId,
            int sequenceNumber,
            String content,
            String modelName,
            String promptVersion,
            String aiRequestId,
            Instant messageCreatedAt) {
        this.sessionId = sessionId;
        this.role = role;
        this.senderUserId = senderUserId;
        this.sequenceNumber = sequenceNumber;
        this.content = content;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.aiRequestId = aiRequestId;
        this.messageCreatedAt = messageCreatedAt;
    }

    static ChatMessageEntity from(ChatMessage message) {
        ChatMessageEntity entity =
                new ChatMessageEntity(
                        message.sessionId(),
                        message.role(),
                        message.senderUserId(),
                        message.sequenceNumber(),
                        message.content(),
                        message.modelName(),
                        message.promptVersion(),
                        message.aiRequestId(),
                        message.createdAt());
        entity.setId(message.id());
        return entity;
    }

    ChatMessage toDomain() {
        return ChatMessage.of(
                getId(),
                sessionId,
                role,
                senderUserId,
                sequenceNumber,
                content,
                modelName,
                promptVersion,
                aiRequestId,
                messageCreatedAt);
    }
}
