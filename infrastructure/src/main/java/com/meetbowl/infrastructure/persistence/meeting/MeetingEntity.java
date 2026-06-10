package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingStatus;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/**
 * 회의 JPA Entity다. {@code meeting} 테이블과 1:1로 매핑되며 infrastructure 내부 모델로만 사용한다(API/Application/Domain
 * 비노출). 사용자/회의실 참조는 모듈 간 직접 연관 대신 raw UUID 컬럼으로 둔다.
 */
@Entity
@Table(
        name = "meeting",
        indexes = {
            @Index(name = "idx_meeting_host", columnList = "host_user_id"),
            @Index(name = "idx_meeting_room", columnList = "meeting_room_id")
        })
public class MeetingEntity extends BaseEntity {

    /** 회의 제목. */
    @Column(nullable = false, length = 200)
    private String title;

    /** 예정 시각(UTC). */
    @Column(nullable = false)
    private Instant scheduledAt;

    /** 주최자(FK). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID hostUserId;

    /** 사용 회의실(FK, nullable). 화상회의만 진행하면 null. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID meetingRoomId;

    /** 화상회의 provider 식별자(nullable). */
    @Column(length = 50)
    private String provider;

    /** provider 측 회의방 식별자(nullable). */
    @Column(length = 200)
    private String providerRoomId;

    /** 회의 상태(SCHEDULED/IN_PROGRESS/ENDED/CANCELLED). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    /** 실제 시작 시각(UTC, nullable). */
    @Column private Instant startedAt;

    /** 실제 종료 시각(UTC, nullable). */
    @Column private Instant endedAt;

    protected MeetingEntity() {}

    private MeetingEntity(
            String title,
            Instant scheduledAt,
            UUID hostUserId,
            UUID meetingRoomId,
            String provider,
            String providerRoomId,
            MeetingStatus status,
            Instant startedAt,
            Instant endedAt) {
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.hostUserId = hostUserId;
        this.meetingRoomId = meetingRoomId;
        this.provider = provider;
        this.providerRoomId = providerRoomId;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    static MeetingEntity from(Meeting meeting) {
        MeetingEntity entity =
                new MeetingEntity(
                        meeting.title(),
                        meeting.scheduledAt(),
                        meeting.hostUserId(),
                        meeting.meetingRoomId(),
                        meeting.provider(),
                        meeting.providerRoomId(),
                        meeting.status(),
                        meeting.startedAt(),
                        meeting.endedAt());
        entity.setId(meeting.id());
        return entity;
    }

    Meeting toDomain() {
        return Meeting.of(
                getId(),
                title,
                scheduledAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                status,
                startedAt,
                endedAt);
    }
}