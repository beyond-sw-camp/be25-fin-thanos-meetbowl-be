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

    /** 회의 투영 일정이 개인 캘린더 삭제 경로를 통해 원본과 분리되는 것을 막는다. */
    boolean deletePersonalByIdAndOwnerUserId(UUID eventId, UUID ownerUserId);

    // 유니크 제약(owner_user_id, source, source_id) 덕에 사용자·회의당 최대 1건이라, 멱등 upsert 조회는 단건이다.
    Optional<PersonalWorkspaceCalendarEvent> findByOwnerUserIdAndSourceAndSourceId(
            UUID ownerUserId, CalendarEventSource source, UUID sourceId);

    /** 회의 취소 시 해당 회의로 투영된 모든 사용자 일정을 한 번에 제거한다. */
    int deleteBySourceIdAndSource(UUID sourceId, CalendarEventSource source);
}
