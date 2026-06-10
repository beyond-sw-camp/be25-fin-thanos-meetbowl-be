package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.CalendarEventSource;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

@Repository
public class JpaPersonalWorkspaceCalendarEventRepositoryAdapter
        implements PersonalWorkspaceCalendarEventRepositoryPort {

    private final SpringDataPersonalWorkspaceCalendarEventRepository repository;

    public JpaPersonalWorkspaceCalendarEventRepositoryAdapter(
            SpringDataPersonalWorkspaceCalendarEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public PersonalWorkspaceCalendarEvent save(PersonalWorkspaceCalendarEvent event) {
        return repository.save(PersonalWorkspaceCalendarEventEntity.from(event)).toDomain();
    }

    @Override
    public Optional<PersonalWorkspaceCalendarEvent> findByIdAndOwnerUserId(
            UUID eventId, UUID ownerUserId) {
        return repository
                .findByIdAndOwnerUserId(eventId, ownerUserId)
                .map(PersonalWorkspaceCalendarEventEntity::toDomain);
    }

    @Override
    public List<PersonalWorkspaceCalendarEvent> findByOwnerUserIdAndPeriod(
            UUID ownerUserId, Instant startedAt, Instant endedAt) {
        return repository
                .findByOwnerUserIdAndStartedAtLessThanAndEndedAtGreaterThanOrderByStartedAtAsc(
                        ownerUserId, endedAt, startedAt)
                .stream()
                .map(PersonalWorkspaceCalendarEventEntity::toDomain)
                .toList();
    }

    @Override
    public List<PersonalWorkspaceCalendarEvent> findVisibleByUserIdAndPeriod(
            UUID userId, Instant startedAt, Instant endedAt) {
        return repository.findVisibleByUserIdAndPeriod(userId, startedAt, endedAt).stream()
                .map(PersonalWorkspaceCalendarEventEntity::toDomain)
                .toList();
    }

    @Override
    public boolean deletePersonalByIdAndOwnerUserId(UUID eventId, UUID ownerUserId) {
        return repository.deleteByIdAndOwnerUserIdAndSource(
                        eventId, ownerUserId, CalendarEventSource.PERSONAL)
                > 0;
    }
}
