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

import com.meetbowl.domain.meeting.MeetingProvider;
import com.meetbowl.domain.meeting.MeetingSession;
import com.meetbowl.domain.meeting.MeetingSessionStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * Meetbowl 회의와 외부 미디어 공급자의 회의방 연결 정보를 저장하는 JPA Entity다.
 *
 * <p>meeting_id는 회의당 진행 세션 하나를 보장하고, provider와 provider_room_id 조합은 같은 외부 방의 중복 등록을 막는다. 다른 기능
 * Entity와 직접 연관관계를 맺지 않고 UUID를 저장해 기능 간 결합을 낮춘다.
 */
@Entity
@Table(
        name = "meeting_sessions",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_meeting_sessions_meeting", columnNames = "meeting_id"),
            @UniqueConstraint(
                    name = "uk_meeting_sessions_provider_room",
                    columnNames = {"provider", "provider_room_id"})
        },
        indexes = {
            @Index(
                    name = "idx_meeting_sessions_organization_status",
                    columnList = "organization_id, status")
        })
public class MeetingSessionEntity extends BaseEntity {

    /** 상위 회의 도메인 ID다. 회의 생성 후 바뀌지 않으므로 수정 불가 컬럼으로 저장한다. */
    @Column(
            name = "meeting_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID meetingId;

    /** 조직 범위 조회와 접근 권한 확인에 사용하는 조직 ID 스냅샷이다. */
    @Column(
            name = "organization_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID organizationId;

    /** 회의 진행 권한을 가진 주최자 사용자 ID 스냅샷이다. */
    @Column(
            name = "host_user_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID hostUserId;

    /** 공급자 종류를 문자열로 저장해 enum 순서 변경이 DB 값에 영향을 주지 않도록 한다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingProvider provider;

    /** LiveKit room name처럼 공급자 시스템에서 실제 방을 찾는 식별자다. */
    @Column(name = "provider_room_id", nullable = false, length = 255)
    private String providerRoomId;

    /** 입장 허용과 종료 여부를 판단하는 Meetbowl 기준 세션 상태다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingSessionStatus status;

    /** 회의방을 참가자에게 개방한 시각이다. 아직 개방되지 않았다면 null이다. */
    @Column(name = "opened_at")
    private Instant openedAt;

    /** 실제 회의가 시작된 시각이다. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** 회의가 종료된 시각이다. 종료 사유 컬럼은 요구사항에 따라 두지 않는다. */
    @Column(name = "ended_at")
    private Instant endedAt;

    /** JPA가 조회 결과를 객체로 만들 때 사용하는 기본 생성자다. 외부 직접 생성을 막기 위해 protected로 제한한다. */
    protected MeetingSessionEntity() {}

    private MeetingSessionEntity(
            UUID meetingId,
            UUID organizationId,
            UUID hostUserId,
            MeetingProvider provider,
            String providerRoomId,
            MeetingSessionStatus status,
            Instant openedAt,
            Instant startedAt,
            Instant endedAt) {
        this.meetingId = meetingId;
        this.organizationId = organizationId;
        this.hostUserId = hostUserId;
        this.provider = provider;
        this.providerRoomId = providerRoomId;
        this.status = status;
        this.openedAt = openedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    /**
     * 검증이 끝난 Domain Model을 DB 저장 전용 Entity로 변환한다.
     *
     * <p>기존 ID가 있으면 BaseEntity에 복사해 update로 처리하고, 신규 모델의 null ID는 JPA가 생성하도록 둔다.
     */
    static MeetingSessionEntity from(MeetingSession meetingSession) {
        MeetingSessionEntity entity =
                new MeetingSessionEntity(
                        meetingSession.meetingId(),
                        meetingSession.organizationId(),
                        meetingSession.hostUserId(),
                        meetingSession.provider(),
                        meetingSession.providerRoomId(),
                        meetingSession.status(),
                        meetingSession.openedAt(),
                        meetingSession.startedAt(),
                        meetingSession.endedAt());
        entity.setId(meetingSession.id());
        return entity;
    }

    /**
     * 조회한 Entity 값을 Domain 팩토리로 전달해 업무 불변조건이 검증된 모델로 복원한다.
     *
     * <p>Repository Adapter는 이 메서드의 결과만 외부 계층에 반환하므로 Entity가 Infrastructure 밖으로 노출되지 않는다.
     */
    MeetingSession toDomain() {
        return MeetingSession.of(
                getId(),
                meetingId,
                organizationId,
                hostUserId,
                provider,
                providerRoomId,
                status,
                openedAt,
                startedAt,
                endedAt);
    }
}
