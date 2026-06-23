package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.calendar.CalendarEventResult;

/** 개인 캘린더 일정 응답 DTO다. source가 MEETING이면 회의 투영 일정이므로 화면에서 직접 수정/삭제를 막는 데 사용한다. */
public record CalendarEventResponse(
        UUID eventId,
        UUID ownerUserId,
        String title,
        String description,
        Instant startedAt,
        Instant endedAt,
        boolean allDay,
        String source,
        UUID sourceId) {

    public static CalendarEventResponse from(CalendarEventResult result) {
        return new CalendarEventResponse(
                result.eventId(),
                result.ownerUserId(),
                result.title(),
                result.description(),
                result.startedAt(),
                result.endedAt(),
                result.allDay(),
                result.source(),
                result.sourceId());
    }
}
