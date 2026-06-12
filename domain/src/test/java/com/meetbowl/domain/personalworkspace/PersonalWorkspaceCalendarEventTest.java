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
                                        null));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void personalCalendarEventRejectsSourceReference() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                PersonalWorkspaceCalendarEvent.of(
                                        null,
                                        UUID.randomUUID(),
                                        "개인 일정",
                                        null,
                                        Instant.parse("2099-01-01T01:00:00Z"),
                                        Instant.parse("2099-01-01T02:00:00Z"),
                                        false,
                                        CalendarEventSource.PERSONAL,
                                        UUID.randomUUID()));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void createMeetingCalendarEvent() {
        UUID meetingId = UUID.randomUUID();

        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createFromMeeting(
                        UUID.randomUUID(),
                        meetingId,
                        "주간 회의",
                        "회의실 A",
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T02:00:00Z"));

        assertEquals(CalendarEventSource.MEETING, event.source());
        assertEquals(meetingId, event.sourceId());
    }

    @Test
    void meetingCalendarEventCannotBeUpdatedDirectly() {
        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createFromMeeting(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "주간 회의",
                        null,
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T02:00:00Z"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                event.updatePersonal(
                                        "임의 수정",
                                        null,
                                        Instant.parse("2099-01-01T03:00:00Z"),
                                        Instant.parse("2099-01-01T04:00:00Z"),
                                        false));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    @Test
    void meetingCalendarEventCanBeSynchronizedFromMeeting() {
        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createFromMeeting(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "변경 전",
                        null,
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T02:00:00Z"));

        PersonalWorkspaceCalendarEvent synchronizedEvent =
                event.syncFromMeeting(
                        "변경 후",
                        "회의실 B",
                        Instant.parse("2099-01-01T03:00:00Z"),
                        Instant.parse("2099-01-01T04:00:00Z"));

        assertEquals("변경 후", synchronizedEvent.title());
        assertEquals(event.sourceId(), synchronizedEvent.sourceId());
        assertEquals(event.ownerUserId(), synchronizedEvent.ownerUserId());
    }
}
