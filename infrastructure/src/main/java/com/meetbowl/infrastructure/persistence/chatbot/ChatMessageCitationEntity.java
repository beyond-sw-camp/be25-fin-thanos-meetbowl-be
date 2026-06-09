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

import com.meetbowl.domain.chatbot.ChatMessageCitation;
import com.meetbowl.domain.chatbot.ChatSourceType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "chat_message_citations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_chat_message_citation_order",
                        columnNames = {"message_id", "display_order"}),
        indexes = @Index(name = "idx_chat_message_citation_message", columnList = "message_id"))
public class ChatMessageCitationEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChatSourceType sourceType;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    // 공유 워크스페이스 자료는 답변 생성에 사용된 파일 버전 ID를 저장한다.
    private UUID sourceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String snippet;

    @Column(length = 500)
    private String sourceUri;

    private Double score;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private Instant citedAt;

    protected ChatMessageCitationEntity() {}

    private ChatMessageCitationEntity(
            UUID messageId,
            ChatSourceType sourceType,
            UUID sourceId,
            String title,
            String snippet,
            String sourceUri,
            Double score,
            int displayOrder,
            Instant citedAt) {
        this.messageId = messageId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.snippet = snippet;
        this.sourceUri = sourceUri;
        this.score = score;
        this.displayOrder = displayOrder;
        this.citedAt = citedAt;
    }

    static ChatMessageCitationEntity from(ChatMessageCitation citation) {
        ChatMessageCitationEntity entity =
                new ChatMessageCitationEntity(
                        citation.messageId(),
                        citation.sourceType(),
                        citation.sourceId(),
                        citation.title(),
                        citation.snippet(),
                        citation.sourceUri(),
                        citation.score(),
                        citation.displayOrder(),
                        citation.citedAt());
        entity.setId(citation.id());
        return entity;
    }

    ChatMessageCitation toDomain() {
        return ChatMessageCitation.of(
                getId(),
                messageId,
                sourceType,
                sourceId,
                title,
                snippet,
                sourceUri,
                score,
                displayOrder,
                citedAt);
    }
}
