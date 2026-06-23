package com.meetbowl.application.personalworkspace.calendar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEvent;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/**
 * 개인 일정을 수정한다.
 *
 * <p>소유자 기준 조회로 본인 일정만 다루고, 회의 투영 일정 수정은 도메인 {@code updatePersonal}이 차단한다. 따라서 이 UseCase는 별도 출처 분기
 * 없이 도메인 규칙에 위임한다.
 */
@Service
public class UpdateCalendarEventUseCase {

    private final PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort;

    public UpdateCalendarEventUseCase(
            PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort) {
        this.calendarEventRepositoryPort = calendarEventRepositoryPort;
    }

    @Transactional
    public CalendarEventResult execute(UpdateCalendarEventCommand command) {
        PersonalWorkspaceCalendarEvent event =
                calendarEventRepositoryPort
                        .findByIdAndOwnerUserId(command.eventId(), command.ownerUserId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "개인 일정을 찾을 수 없습니다."));

        PersonalWorkspaceCalendarEvent updated =
                event.updatePersonal(
                        command.title(),
                        command.description(),
                        command.startedAt(),
                        command.endedAt(),
                        command.allDay());

        return CalendarEventResult.from(calendarEventRepositoryPort.save(updated));
    }
}
