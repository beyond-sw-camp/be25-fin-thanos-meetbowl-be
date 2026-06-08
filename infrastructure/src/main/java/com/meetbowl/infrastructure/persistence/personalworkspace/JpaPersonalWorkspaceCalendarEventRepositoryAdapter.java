package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

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
    public Optional<PersonalWorkspaceCalendarEvent> findById(UUID eventId) {
        return repository.findById(eventId).map(PersonalWorkspaceCalendarEventEntity::toDomain);
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
    public void deleteById(UUID eventId) {
        repository.deleteById(eventId);
    }
}
