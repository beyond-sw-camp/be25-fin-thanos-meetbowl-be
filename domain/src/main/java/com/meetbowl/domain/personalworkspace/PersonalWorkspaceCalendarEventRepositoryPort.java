package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceCalendarEventRepositoryPort {

    PersonalWorkspaceCalendarEvent save(PersonalWorkspaceCalendarEvent event);

    Optional<PersonalWorkspaceCalendarEvent> findByIdAndOwnerUserId(UUID eventId, UUID ownerUserId);

    List<PersonalWorkspaceCalendarEvent> findByOwnerUserIdAndPeriod(
            UUID ownerUserId, Instant startedAt, Instant endedAt);

    List<PersonalWorkspaceCalendarEvent> findVisibleByUserIdAndPeriod(
            UUID userId, Instant startedAt, Instant endedAt);

    boolean deleteByIdAndOwnerUserId(UUID eventId, UUID ownerUserId);
}
