package com.meetbowl.application.personalworkspace.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.meetbowl.application.meeting.MeetingCalendarSyncCommand;
import com.meetbowl.domain.personalworkspace.CalendarEventSource;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/** 회의 일정의 참석자별 멱등 투영(있으면 갱신, 없으면 생성)과 취소 시 일괄 삭제를 검증한다. */
class MeetingCalendarSyncAdapterTest {

    private final Instant startedAt = Instant.parse("2099-01-01T01:00:00Z");
    private final Instant endedAt = Instant.parse("2099-01-01T02:00:00Z");

    private final PersonalWorkspaceCalendarEventRepositoryPort repository =
            mock(PersonalWorkspaceCalendarEventRepositoryPort.class);
    private final MeetingCalendarSyncAdapter adapter = new MeetingCalendarSyncAdapter(repository);

    @Test
    void createsMeetingEventForEachAttendeeWhenNoneExist() {
        UUID meetingId = UUID.randomUUID();
        UUID attendee1 = UUID.randomUUID();
        UUID attendee2 = UUID.randomUUID();
        when(repository.findByOwnerUserIdAndSourceAndSourceId(
                        any(), eq(CalendarEventSource.MEETING), eq(meetingId)))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.syncFromMeeting(
                new MeetingCalendarSyncCommand(
                        meetingId, List.of(attendee1, attendee2), "주간 회의", null, startedAt, endedAt));

        ArgumentCaptor<PersonalWorkspaceCalendarEvent> captor =
                ArgumentCaptor.forClass(PersonalWorkspaceCalendarEvent.class);
        verify(repository, times(2)).save(captor.capture());

        List<PersonalWorkspaceCalendarEvent> saved = captor.getAllValues();
        assertThat(saved)
                .allSatisfy(
                        event -> {
                            assertThat(event.source()).isEqualTo(CalendarEventSource.MEETING);
                            assertThat(event.sourceId()).isEqualTo(meetingId);
                            assertThat(event.title()).isEqualTo("주간 회의");
                            assertThat(event.startedAt()).isEqualTo(startedAt);
                            assertThat(event.endedAt()).isEqualTo(endedAt);
                        });
        assertThat(saved)
                .extracting(PersonalWorkspaceCalendarEvent::ownerUserId)
                .containsExactlyInAnyOrder(attendee1, attendee2);
    }

    @Test
    void updatesExistingEventInsteadOfCreatingDuplicate() {
        UUID meetingId = UUID.randomUUID();
        UUID attendee = UUID.randomUUID();
        // 회의 수정 재호출을 모사: 같은 (소유자, MEETING, meetingId) 일정이 이미 있다고 가정한다.
        PersonalWorkspaceCalendarEvent existing =
                PersonalWorkspaceCalendarEvent.createFromMeeting(
                        attendee,
                        meetingId,
                        "이전 제목",
                        null,
                        Instant.parse("2098-12-31T00:00:00Z"),
                        Instant.parse("2098-12-31T01:00:00Z"));
        when(repository.findByOwnerUserIdAndSourceAndSourceId(
                        attendee, CalendarEventSource.MEETING, meetingId))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.syncFromMeeting(
                new MeetingCalendarSyncCommand(
                        meetingId, List.of(attendee), "변경된 제목", null, startedAt, endedAt));

        ArgumentCaptor<PersonalWorkspaceCalendarEvent> captor =
                ArgumentCaptor.forClass(PersonalWorkspaceCalendarEvent.class);
        verify(repository, times(1)).save(captor.capture());

        PersonalWorkspaceCalendarEvent saved = captor.getValue();
        assertThat(saved.title()).isEqualTo("변경된 제목");
        assertThat(saved.startedAt()).isEqualTo(startedAt);
        assertThat(saved.endedAt()).isEqualTo(endedAt);
        // 식별 관계(소유자/출처/출처ID)는 갱신 후에도 유지돼야 중복·분리가 생기지 않는다.
        assertThat(saved.ownerUserId()).isEqualTo(attendee);
        assertThat(saved.source()).isEqualTo(CalendarEventSource.MEETING);
        assertThat(saved.sourceId()).isEqualTo(meetingId);
    }

    @Test
    void removeMeetingEventsDeletesAllMeetingProjections() {
        UUID meetingId = UUID.randomUUID();

        adapter.removeMeetingEvents(meetingId);

        verify(repository).deleteBySourceIdAndSource(meetingId, CalendarEventSource.MEETING);
    }
}
