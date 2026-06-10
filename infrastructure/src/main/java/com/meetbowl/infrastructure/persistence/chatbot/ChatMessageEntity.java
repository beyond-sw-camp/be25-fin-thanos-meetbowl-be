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

/**
 * {@link ChatMessage}를 {@code chat_messages} 테이블에 저장하는 JPA Entity다.
 *
 * <p>한 세션 안에서 {@code sequence_number}가 중복되면 대화 순서를 결정할 수 없으므로 세션 ID와 메시지 순번에 유니크 제약을 둔다. 세션 ID 인덱스는
 * 전체 대화 이력을 순번대로 조회할 때 사용한다.
 */
@Entity
@Table(
        name = "chat_messages",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_chat_message_session_sequence",
                        columnNames = {"session_id", "sequence_number"}),
        indexes = @Index(name = "idx_chat_message_session", columnList = "session_id"))
public class ChatMessageEntity extends BaseEntity {

    /** 메시지가 속한 챗봇 세션 ID다. 세션과 메시지는 1:N 관계다. */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    /** 사용자, AI, 시스템 메시지를 구분하며 Enum 이름을 문자열로 저장한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatMessageRole role;

    /** 사용자 메시지 작성자 ID다. ASSISTANT와 SYSTEM 메시지에는 null을 저장한다. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID senderUserId;

    /** 세션 안의 대화 순서를 보장하는 1부터 시작하는 순번이다. */
    @Column(nullable = false)
    private int sequenceNumber;

    /** 메시지 본문이다. 질문과 답변 길이를 고려해 최대 20,000자로 제한한다. */
    @Column(nullable = false, length = 20000)
    private String content;

    /** AI 답변 생성 모델명이다. 사용자 메시지에는 값이 없다. */
    @Column(length = 100)
    private String modelName;

    /** 답변 생성에 적용한 프롬프트 버전으로, 결과 재현과 품질 비교에 사용한다. */
    @Column(length = 50)
    private String promptVersion;

    /** meetbowl-ai 또는 AI Provider 요청 추적 ID다. */
    @Column(length = 100)
    private String aiRequestId;

    /** 실제 메시지 생성 시각이다. BaseEntity의 DB 생성 시각과 구분한다. */
    @Column(nullable = false)
    private Instant messageCreatedAt;

    /** JPA 전용 기본 생성자다. */
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

    /** 저장할 도메인 메시지를 영속성 모델로 변환한다. */
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

    /** 조회한 Entity를 도메인 메시지로 복원하고 도메인 검증을 다시 적용한다. */
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
