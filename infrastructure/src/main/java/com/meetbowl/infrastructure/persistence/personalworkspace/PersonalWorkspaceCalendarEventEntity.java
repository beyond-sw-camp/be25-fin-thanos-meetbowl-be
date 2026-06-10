package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.CalendarEventSource;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "personal_workspace_calendar_events",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_calendar_event_owner_source",
                        columnNames = {"owner_user_id", "source", "source_id"}))
public class PersonalWorkspaceCalendarEventEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endedAt;

    @Column(nullable = false)
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalendarEventSource source;

    @Column(columnDefinition = "BINARY(16)")
    private UUID sourceId;

    protected PersonalWorkspaceCalendarEventEntity() {}

    private PersonalWorkspaceCalendarEventEntity(
            UUID ownerUserId,
            String title,
            String description,
            Instant startedAt,
            Instant endedAt,
            boolean allDay,
            CalendarEventSource source,
            UUID sourceId) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.allDay = allDay;
        this.source = source;
        this.sourceId = sourceId;
    }

    static PersonalWorkspaceCalendarEventEntity from(PersonalWorkspaceCalendarEvent event) {
        PersonalWorkspaceCalendarEventEntity entity =
                new PersonalWorkspaceCalendarEventEntity(
                        event.ownerUserId(),
                        event.title(),
                        event.description(),
                        event.startedAt(),
                        event.endedAt(),
                        event.allDay(),
                        event.source(),
                        event.sourceId());
        entity.setId(event.id());
        return entity;
    }

    PersonalWorkspaceCalendarEvent toDomain() {
        return PersonalWorkspaceCalendarEvent.of(
                getId(),
                ownerUserId,
                title,
                description,
                startedAt,
                endedAt,
                allDay,
                source,
                sourceId);
    }
}
