package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.CalendarEventSource;

/**
 * 개인 캘린더 일정 엔티티의 Spring Data JPA 리포지토리다.
 *
 * <p>가시성 조회({@code findVisibleByUserIdAndPeriod})는 본인 일정과 구독 중인 동료 일정을 한 쿼리로 모은다. 직접 삭제는 출처가
 * PERSONAL인 일정으로만 제한해 회의 파생 일정을 보호한다.
 */
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
    long deleteByIdAndOwnerUserIdAndSource(
            UUID eventId, UUID ownerUserId, CalendarEventSource source);
}
