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

/**
 * {@link ChatMessageCitation}을 {@code chat_message_citations} 테이블에 저장하는 JPA Entity다.
 *
 * <p>한 AI 답변 메시지에 여러 출처가 연결되는 1:N 구조다. {@code message_id + display_order} 유니크 제약으로 같은 답변 안에서 출처 표시
 * 순서가 중복되는 것을 방지하고, 메시지 ID 인덱스로 답변별 출처 목록을 빠르게 조회한다.
 *
 * <p>이 테이블은 원문 전체나 사용자 권한을 저장하지 않는다. 답변 생성 당시의 출처 식별 정보와 짧은 근거만 보존하며, 현재 접근 권한은 조회 시점에 별도로 검증한다.
 */
@Entity
@Table(
        name = "chat_message_citations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_chat_message_citation_order",
                        columnNames = {"message_id", "display_order"}),
        indexes = @Index(name = "idx_chat_message_citation_message", columnList = "message_id"))
public class ChatMessageCitationEntity extends BaseEntity {

    /** 출처를 사용한 ASSISTANT 메시지 ID다. */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID messageId;

    /** 원본 자료 유형이며 Enum 이름을 문자열로 저장한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChatSourceType sourceType;

    /** 개인 드라이브는 파일 ID, 공유 워크스페이스는 답변 생성에 실제 사용한 파일 버전 ID를 저장한다. */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID sourceId;

    /** 답변 생성 당시 출처 표시용 제목이다. */
    @Column(nullable = false, length = 255)
    private String title;

    /** 답변 근거가 된 짧은 원문 발췌이며 원문 전체를 저장하지 않는다. */
    @Column(nullable = false, length = 2000)
    private String snippet;

    /** 원본 화면 이동용 내부 URI다. 권한을 잃은 사용자에게는 API 응답에서 마스킹한다. */
    @Column(length = 500)
    private String sourceUri;

    /** 검색 관련도 점수다. 검색 엔진이 제공하지 않으면 null일 수 있다. */
    private Double score;

    /** 같은 답변 안에서 출처를 표시할 순번이다. */
    @Column(nullable = false)
    private int displayOrder;

    /** 이 출처가 AI 답변 근거로 채택된 UTC 시각이다. */
    @Column(nullable = false)
    private Instant citedAt;

    /** JPA 전용 기본 생성자다. */
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

    /** 도메인 출처를 JPA Entity로 변환한다. */
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

    /** 조회한 Entity를 도메인 출처로 복원하고 길이·점수·표시 순서 불변식을 다시 검증한다. */
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
