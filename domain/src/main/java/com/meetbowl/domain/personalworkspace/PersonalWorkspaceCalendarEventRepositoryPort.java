package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceCalendarEventRepositoryPort {

    PersonalWorkspaceCalendarEvent save(PersonalWorkspaceCalendarEvent event);

    Optional<PersonalWorkspaceCalendarEvent> findById(UUID eventId);

    List<PersonalWorkspaceCalendarEvent> findByOwnerUserIdAndPeriod(
            UUID ownerUserId, Instant startedAt, Instant endedAt);

    void deleteById(UUID eventId);
}
