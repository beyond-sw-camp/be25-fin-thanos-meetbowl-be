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

/**
 * {@link ChatSession}을 {@code chat_sessions} 테이블에 저장하는 JPA Entity다.
 *
 * <p>도메인 모델이 JPA에 의존하지 않도록 영속성 전용 클래스로 분리했다. 소유자와 상태 복합 인덱스는 사용자의 활성 세션 목록 조회에, 범위 인덱스는 특정
 * 회의·회의록·워크스페이스에 연결된 세션 조회에 사용한다.
 */
@Entity
@Table(
        name = "chat_sessions",
        indexes = {
            @Index(name = "idx_chat_session_owner_status", columnList = "owner_user_id,status"),
            @Index(name = "idx_chat_session_scope", columnList = "scope_type,scope_id")
        })
public class ChatSessionEntity extends BaseEntity {

    /** 세션 소유 사용자 ID다. 사용자 Entity를 직접 참조하지 않고 UUID만 저장해 기능 간 Entity 결합을 피한다. */
    @Column(name = "owner_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    /** 대화 목록에 표시할 제목이다. */
    @Column(length = 100)
    private String title;

    /** 검색 범위 유형은 Enum 이름을 문자열로 저장해 ordinal 변경 위험을 방지한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChatScopeType scopeType;

    /** GENERAL 세션에서는 null이고, 그 외 범위에서는 대상 리소스 UUID를 저장한다. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID scopeId;

    /** 활성 또는 soft delete 상태다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatSessionStatus status;

    /** 대화 세션이 최초 생성된 도메인 시각이다. */
    @Column(nullable = false)
    private Instant startedAt;

    /** 최근 메시지 기준 활동 시각이며 세션 목록 정렬에 사용한다. */
    @Column(nullable = false)
    private Instant lastMessageAt;

    /** soft delete 시각이다. 활성 세션에는 값이 없다. */
    private Instant deletedAt;

    /** JPA가 조회 결과를 객체로 복원할 때 사용하는 기본 생성자다. 애플리케이션에서 직접 호출하지 않는다. */
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

    /** 도메인 모델을 JPA Entity로 변환한다. 기존 ID가 있으면 그대로 유지하고 신규 객체면 BaseEntity가 저장 시 ID를 생성한다. */
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

    /** DB 조회 결과를 도메인 모델로 복원하며 {@link ChatSession#of}의 불변식 검증을 다시 적용한다. */
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
