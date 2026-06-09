package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface SpringDataPersonalWorkspaceCalendarEventRepository
        extends JpaRepository<PersonalWorkspaceCalendarEventEntity, UUID> {

    Optional<PersonalWorkspaceCalendarEventEntity> findByIdAndOwnerUserId(
            UUID eventId, UUID ownerUserId);

    List<PersonalWorkspaceCalendarEventEntity>
            findByOwnerUserIdAndStartedAtLessThanAndEndedAtGreaterThanOrderByStartedAtAsc(
                    UUID ownerUserId, Instant endedAt, Instant startedAt);

    @Query(
            """
            SELECT event
            FROM PersonalWorkspaceCalendarEventEntity event
            WHERE (
                event.ownerUserId = :userId
                OR event.ownerUserId IN (
                    SELECT subscription.targetUserId
                    FROM PersonalWorkspaceCalendarSubscriptionEntity subscription
                    WHERE subscription.subscriberUserId = :userId
                )
            )
            AND event.startedAt < :endedAt
            AND event.endedAt > :startedAt
            ORDER BY event.startedAt ASC
            """)
    List<PersonalWorkspaceCalendarEventEntity> findVisibleByUserIdAndPeriod(
            @Param("userId") UUID userId,
            @Param("startedAt") Instant startedAt,
            @Param("endedAt") Instant endedAt);

    @Transactional
    long deleteByIdAndOwnerUserId(UUID eventId, UUID ownerUserId);
}
