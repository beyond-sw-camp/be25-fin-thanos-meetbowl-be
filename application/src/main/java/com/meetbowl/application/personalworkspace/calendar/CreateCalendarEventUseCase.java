package com.meetbowl.application.personalworkspace.calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/** 제목·기간 검증과 PERSONAL 출처 고정은 도메인 팩토리에 위임한다. */
@Service
public class CreateCalendarEventUseCase {

    private final PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort;

    public CreateCalendarEventUseCase(
            PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort) {
        this.calendarEventRepositoryPort = calendarEventRepositoryPort;
    }

    @Transactional
    public CalendarEventResult execute(CreateCalendarEventCommand command) {
        PersonalWorkspaceCalendarEvent event =
                PersonalWorkspaceCalendarEvent.createPersonal(
                        command.ownerUserId(),
                        command.title(),
                        command.description(),
                        command.startedAt(),
                        command.endedAt(),
                        command.allDay());

        return CalendarEventResult.from(calendarEventRepositoryPort.save(event));
    }
}
