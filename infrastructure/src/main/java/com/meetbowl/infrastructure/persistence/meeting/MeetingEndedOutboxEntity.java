package com.meetbowl.infrastructure.persistence.meeting;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** meeting.ended의 발행 보장을 위한 Transactional Outbox Entity다. */
@Entity
@Table(
        name = "meeting_ended_outbox",
        indexes = {
            @Index(
                    name = "idx_meeting_ended_outbox_ready",
                    columnList = "next_attempt_at, created_at")
        })
public class MeetingEndedOutboxEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID correlationId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID organizationId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID hostUserId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID reviewerUserId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endedAt;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private int publishAttempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 500)
    private String lastFailureReason;

    protected MeetingEndedOutboxEntity() {}

    static MeetingEndedOutboxEntity from(MeetingEndedEvent event) {
        MeetingEndedOutboxEntity entity = new MeetingEndedOutboxEntity();
        entity.setId(event.eventId());
        entity.correlationId = event.correlationId();
        entity.meetingId = event.meetingId();
        entity.organizationId = event.organizationId();
        entity.hostUserId = event.hostUserId();
        entity.reviewerUserId = event.reviewerUserId();
        entity.title = event.title();
        entity.startedAt = event.startedAt();
        entity.endedAt = event.endedAt();
        entity.occurredAt = event.occurredAt();
        entity.publishAttempts = event.publishAttempts();
        entity.nextAttemptAt = event.occurredAt();
        return entity;
    }

    MeetingEndedEvent toDomain() {
        return new MeetingEndedEvent(
                getId(),
                correlationId,
                meetingId,
                organizationId,
                hostUserId,
                reviewerUserId,
                title,
                startedAt,
                endedAt,
                occurredAt,
                publishAttempts);
    }

    void markFailed(Instant nextAttemptAt, String failureReason) {
        this.publishAttempts++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastFailureReason = failureReason;
    }
}
