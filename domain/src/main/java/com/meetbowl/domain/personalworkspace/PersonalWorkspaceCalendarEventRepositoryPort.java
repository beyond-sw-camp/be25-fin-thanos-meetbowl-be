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
}
