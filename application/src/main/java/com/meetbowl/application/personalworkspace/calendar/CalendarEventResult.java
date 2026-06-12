package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;

/**
 * 개인 캘린더 일정 UseCase의 출력 모델이다.
 *
 * <p>app-api 계층은 도메인 타입에 의존할 수 없으므로 출처(source)는 도메인 enum 대신 문자열로 노출한다. 회의 투영 일정과 개인 일정을 구분하는
 * source, sourceId를 그대로 전달해 화면이 수정 가능 여부를 판단할 수 있게 한다.
 */
public record CalendarEventResult(
        UUID eventId,
        UUID ownerUserId,
        String title,
        String description,
        Instant startedAt,
        Instant endedAt,
        boolean allDay,
        String source,
        UUID sourceId) {

    public static CalendarEventResult from(PersonalWorkspaceCalendarEvent event) {
        return new CalendarEventResult(
                event.id(),
                event.ownerUserId(),
                event.title(),
                event.description(),
                event.startedAt(),
                event.endedAt(),
                event.allDay(),
                event.source().name(),
                event.sourceId());
    }
}
