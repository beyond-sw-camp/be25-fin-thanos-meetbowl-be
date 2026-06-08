package com.meetbowl.domain.personalworkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class PersonalWorkspaceCalendarEventTest {

    @Test
    void createPersonalCalendarEvent() {
        UUID ownerUserId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2099-01-01T01:00:00Z");
        Instant endedAt = Instant.parse("2099-01-01T02:00:00Z");

        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createPersonal(
                        ownerUserId, "  개인 일정  ", "  설명  ", startedAt, endedAt, false);

        assertEquals(null, event.id());
        assertEquals(ownerUserId, event.ownerUserId());
        assertEquals("개인 일정", event.title());
        assertEquals("설명", event.description());
        assertEquals(CalendarEventSource.PERSONAL, event.source());
        assertTrue(event.isOwnedBy(ownerUserId));
    }

    @Test
    void startedAtMustBeBeforeEndedAt() {
        Instant startedAt = Instant.parse("2099-01-01T02:00:00Z");
        Instant endedAt = Instant.parse("2099-01-01T01:00:00Z");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                PersonalWorkspaceCalendarEvent.createPersonal(
                                        UUID.randomUUID(),
                                        "개인 일정",
                                        null,
                                        startedAt,
                                        endedAt,
                                        false));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void meetingCalendarEventRequiresSourceId() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                PersonalWorkspaceCalendarEvent.of(
                                        null,
                                        UUID.randomUUID(),
                                        "회의",
                                        null,
                                        Instant.parse("2099-01-01T01:00:00Z"),
                                        Instant.parse("2099-01-01T02:00:00Z"),
                                        false,
                                        CalendarEventSource.MEETING,
                                        null,
                                        null));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
