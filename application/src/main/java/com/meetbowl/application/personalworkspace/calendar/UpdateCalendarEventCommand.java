package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;
import java.util.UUID;

/** 개인 일정 수정 UseCase의 입력 모델이다. ownerUserId로 소유자 본인 일정만 수정하도록 조회를 제한한다. */
public record UpdateCalendarEventCommand(
        UUID eventId,
        UUID ownerUserId,
        String title,
        String description,
        Instant startedAt,
        Instant endedAt,
        boolean allDay) {}
