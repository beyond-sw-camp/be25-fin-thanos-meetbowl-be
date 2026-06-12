package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;
import java.util.UUID;

/** 개인 일정 등록 UseCase의 입력 모델이다. 출처는 항상 PERSONAL이므로 Command에 포함하지 않는다. */
public record CreateCalendarEventCommand(
        UUID ownerUserId,
        String title,
        String description,
        Instant startedAt,
        Instant endedAt,
        boolean allDay) {}
