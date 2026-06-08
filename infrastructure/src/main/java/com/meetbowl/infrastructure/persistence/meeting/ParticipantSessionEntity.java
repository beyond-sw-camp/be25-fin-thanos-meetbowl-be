package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.meeting.ParticipantSession;
import com.meetbowl.domain.meeting.ParticipantSessionStatus;
import com.meetbowl.domain.meeting.ParticipantType;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회원 또는 게스트가 외부 미디어 회의방에 접속한 상태를 저장하는 JPA Entity다.
 *
 * <p>동일 회의 세션 안에서 provider_identity를 unique로 두어 같은 LiveKit 참가자 연결이 중복 저장되지 않도록 한다. 장치 상태와 퇴장 시각/사유는
 * 서버 영속화 대상이 아니므로 포함하지 않는다.
 */
@Entity
@Table(
        name = "participant_sessions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_participant_sessions_provider_identity",
                    columnNames = {"meeting_session_id", "provider_identity"})
        },
        indexes = {
            @Index(
                    name = "idx_participant_sessions_room_status",
                    columnList = "meeting_session_id, status"),
            @Index(name = "idx_participant_sessions_meeting", columnList = "meeting_id"),
            @Index(name = "idx_participant_sessions_user", columnList = "user_id"),
            @Index(name = "idx_participant_sessions_guest", columnList = "guest_session_id")
        })
public class ParticipantSessionEntity extends BaseEntity {

    /** 참가자가 접속한 내부 회의 진행 세션 ID다. */
    @Column(
            name = "meeting_session_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingSessionId;

    /** 상위 회의 기준 참가자 조회를 위한 회의 ID 스냅샷이다. */
    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** 회원/게스트 식별자 해석과 회의 내 역할을 결정하며 입장 후 변경하지 않는다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, updatable = false, length = 30)
    private ParticipantType participantType;

    /** 회원 참가자의 사용자 ID다. 게스트 행에서는 null이다. */
    @Column(name = "user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    /** 게스트 권한 검증 결과 식별자다. 별도 Guest Entity와 JPA 연관관계를 만들지 않는다. */
    @Column(name = "guest_session_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID guestSessionId;

    /** 사용자 정보가 변경되어도 회의 당시 표시명을 유지하기 위한 스냅샷이다. */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** LiveKit 참가자 연결을 고유하게 식별하는 identity이며 세션 중 변경하지 않는다. */
    @Column(name = "provider_identity", nullable = false, updatable = false, length = 255)
    private String providerIdentity;

    /** 현재 연결 여부를 판단하는 서버 기준 상태로 문자열 enum으로 저장한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParticipantSessionStatus status;

    /** 외부 미디어 회의방 입장이 완료된 시각이다. */
    @Column(name = "joined_at")
    private Instant joinedAt;

    /** 마지막 연결 상태 갱신 시각으로, 추후 비정상 연결 감지 기준으로 사용할 수 있다. */
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /** JPA 전용 기본 생성자이며 애플리케이션 코드의 직접 생성을 허용하지 않는다. */
    protected ParticipantSessionEntity() {}

    private ParticipantSessionEntity(
            UUID meetingSessionId,
            UUID meetingId,
            ParticipantType participantType,
            UUID userId,
            UUID guestSessionId,
            String displayName,
            String providerIdentity,
            ParticipantSessionStatus status,
            Instant joinedAt,
            Instant lastSeenAt) {
        this.meetingSessionId = meetingSessionId;
        this.meetingId = meetingId;
        this.participantType = participantType;
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.displayName = displayName;
        this.providerIdentity = providerIdentity;
        this.status = status;
        this.joinedAt = joinedAt;
        this.lastSeenAt = lastSeenAt;
    }

    /**
     * 참가자 Domain Model을 저장 가능한 Entity로 변환한다.
     *
     * <p>회원과 게스트 식별자 불변조건은 Domain에서 먼저 검증되며, Entity는 검증된 값을 컬럼에 그대로 매핑한다.
     */
    static ParticipantSessionEntity from(ParticipantSession participantSession) {
        ParticipantSessionEntity entity =
                new ParticipantSessionEntity(
                        participantSession.meetingSessionId(),
                        participantSession.meetingId(),
                        participantSession.participantType(),
                        participantSession.userId(),
                        participantSession.guestSessionId(),
                        participantSession.displayName(),
                        participantSession.providerIdentity(),
                        participantSession.status(),
                        participantSession.joinedAt(),
                        participantSession.lastSeenAt());
        entity.setId(participantSession.id());
        return entity;
    }

    /**
     * DB 값을 참가자 Domain 팩토리로 전달해 유형별 식별자 규칙을 다시 검증한다.
     *
     * <p>잘못된 회원/게스트 식별자 조합이 DB에 존재하더라도 Infrastructure 밖으로 전달되기 전에 실패하도록 한다.
     */
    ParticipantSession toDomain() {
        return ParticipantSession.of(
                getId(),
                meetingSessionId,
                meetingId,
                participantType,
                userId,
                guestSessionId,
                displayName,
                providerIdentity,
                status,
                joinedAt,
                lastSeenAt);
    }
}
