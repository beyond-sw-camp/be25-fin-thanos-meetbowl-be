package com.meetbowl.application.personalworkspace.calendar;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.meeting.MeetingCalendarSyncCommand;
import com.meetbowl.application.meeting.MeetingCalendarSyncPort;
import com.meetbowl.domain.personalworkspace.CalendarEventSource;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/**
 * 회의 모듈의 {@link MeetingCalendarSyncPort}를 구현해 회의 일정을 참석자 개인 캘린더로 투영한다.
 *
 * <p>이 빈이 등록되는 순간부터 회의 생성/수정/취소가 캘린더에 반영된다. (구현체가 없으면 회의 측이 {@code ObjectProvider.ifAvailable}로
 * 건너뛰므로, 이 클래스가 곧 그 "빠진 구현체"다.)
 */
@Service
public class MeetingCalendarSyncAdapter implements MeetingCalendarSyncPort {

    private final PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort;

    public MeetingCalendarSyncAdapter(
            PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort) {
        this.calendarEventRepositoryPort = calendarEventRepositoryPort;
    }

    /**
     * 참석자별로 회의 일정을 멱등하게 반영한다.
     *
     * <p>같은 회의로 재호출돼도 중복이 생기지 않도록 (소유자, MEETING, meetingId)로 기존 일정을 찾아 있으면 갱신, 없으면 생성한다. 회의 수정으로
     * 참석자가 빠진 경우의 잔여 일정 정리는 이 메서드의 책임이 아니다(취소 시 {@link #removeMeetingEvents(UUID)}가 담당).
     */
    @Override
    @Transactional
    public void syncFromMeeting(MeetingCalendarSyncCommand command) {
        for (UUID attendeeUserId : command.attendeeUserIds()) {
            PersonalWorkspaceCalendarEvent event =
                    calendarEventRepositoryPort
                            .findByOwnerUserIdAndSourceAndSourceId(
                                    attendeeUserId,
                                    CalendarEventSource.MEETING,
                                    command.meetingId())
                            .map(
                                    existing ->
                                            existing.syncFromMeeting(
                                                    command.title(),
                                                    command.description(),
                                                    command.startedAt(),
                                                    command.endedAt()))
                            .orElseGet(
                                    () ->
                                            PersonalWorkspaceCalendarEvent.createFromMeeting(
                                                    attendeeUserId,
                                                    command.meetingId(),
                                                    command.title(),
                                                    command.description(),
                                                    command.startedAt(),
                                                    command.endedAt()));

            calendarEventRepositoryPort.save(event);
        }
    }

    /** 회의 취소 시 해당 회의로 투영된 모든 참석자 일정을 제거한다. */
    @Override
    @Transactional
    public void removeMeetingEvents(UUID meetingId) {
        calendarEventRepositoryPort.deleteBySourceIdAndSource(
                meetingId, CalendarEventSource.MEETING);
    }
}
