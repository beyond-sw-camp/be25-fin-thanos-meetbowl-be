package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPersonalWorkspaceCalendarEventRepository
        extends JpaRepository<PersonalWorkspaceCalendarEventEntity, UUID> {

    List<PersonalWorkspaceCalendarEventEntity>
            findByOwnerUserIdAndStartedAtLessThanAndEndedAtGreaterThanOrderByStartedAtAsc(
                    UUID ownerUserId, Instant endedAt, Instant startedAt);
}
