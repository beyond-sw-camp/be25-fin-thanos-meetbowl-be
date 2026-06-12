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
            // 회의실 시간대 겹침 조회(중복 예약 검사)용 복합 인덱스. 회의실 + 예정 시작 시각 범위로 스캔한다.
            @Index(name = "idx_meeting_room_scheduled", columnList = "meeting_room_id, scheduled_at")
        })
public class MeetingEntity extends BaseEntity {

    /** 회의 제목. */
    @Column(nullable = false, length = 200)
    private String title;

    /** 예정 시작 시각(UTC). */
    @Column(nullable = false)
    private Instant scheduledAt;

    /** 예정 종료 시각(UTC). 회의실 시간대 겹침 판정의 종료 경계. */
    @Column(nullable = false)
    private Instant scheduledEndAt;

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

    /** 회의 내용/안건(nullable). 길어질 수 있어 TEXT로 저장한다. */
    @Column(columnDefinition = "TEXT")
    private String description;

    protected MeetingEntity() {}

    private MeetingEntity(
            String title,
            Instant scheduledAt,
            Instant scheduledEndAt,
            UUID hostUserId,
            UUID meetingRoomId,
            String provider,
            String providerRoomId,
            MeetingStatus status,
            Instant startedAt,
            Instant endedAt,
            String description) {
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.scheduledEndAt = scheduledEndAt;
        this.hostUserId = hostUserId;
        this.meetingRoomId = meetingRoomId;
        this.provider = provider;
        this.providerRoomId = providerRoomId;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.description = description;
    }

    static MeetingEntity from(Meeting meeting) {
        MeetingEntity entity =
                new MeetingEntity(
                        meeting.title(),
                        meeting.scheduledAt(),
                        meeting.scheduledEndAt(),
                        meeting.hostUserId(),
                        meeting.meetingRoomId(),
                        meeting.provider(),
                        meeting.providerRoomId(),
                        meeting.status(),
                        meeting.startedAt(),
                        meeting.endedAt(),
                        meeting.description());
        entity.setId(meeting.id());
        return entity;
    }

    Meeting toDomain() {
        return Meeting.of(
                getId(),
                title,
                scheduledAt,
                scheduledEndAt,
                hostUserId,
                meetingRoomId,
                provider,
                providerRoomId,
                status,
                startedAt,
                endedAt,
                description);
    }
}