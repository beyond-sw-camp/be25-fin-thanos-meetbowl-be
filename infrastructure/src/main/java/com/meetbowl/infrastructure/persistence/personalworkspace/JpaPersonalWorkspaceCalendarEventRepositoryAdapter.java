package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.personalworkspace.CalendarEventSource;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/**
 * 개인 캘린더 일정의 {@link PersonalWorkspaceCalendarEventRepositoryPort}를 JPA로 구현한다.
 *
 * <p>기간 조회는 일정 구간이 조회 구간과 겹치는지로 판정한다. 직접 삭제는 {@code PERSONAL} 출처 일정으로만 제한해, 회의에서 파생된 일정은 캘린더 API로
 * 지울 수 없게 한다. 동료 일정은 구독 가시성 쿼리로만 노출한다.
 */
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

    @Override
    public Optional<PersonalWorkspaceCalendarEvent> findByOwnerUserIdAndSourceAndSourceId(
            UUID ownerUserId, CalendarEventSource source, UUID sourceId) {
        return repository
                .findByOwnerUserIdAndSourceAndSourceId(ownerUserId, source, sourceId)
                .map(PersonalWorkspaceCalendarEventEntity::toDomain);
    }

    @Override
    public int deleteBySourceIdAndSource(UUID sourceId, CalendarEventSource source) {
        // Spring Data 삭제 카운트는 long이지만 포트 계약은 int라 좁혀서 반환한다.
        return (int) repository.deleteBySourceIdAndSource(sourceId, source);
    }
}
