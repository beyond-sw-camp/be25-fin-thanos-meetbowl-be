package com.meetbowl.infrastructure.persistence.minutes;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.infrastructure.persistence.common.BaseEntity;

/** minutes.generated Consumer가 처리한 eventId를 보존하는 durable inbox Entity다. */
@Entity
@Table(
        name = "minutes_generated_event_inbox",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_minutes_generated_event_id",
                        columnNames = "event_id"))
public class MinutesGeneratedEventEntity extends BaseEntity {

    @Column(name = "event_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "meeting_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID meetingId;

    protected MinutesGeneratedEventEntity() {}

    MinutesGeneratedEventEntity(UUID eventId, UUID meetingId) {
        this.eventId = eventId;
        this.meetingId = meetingId;
    }
}
