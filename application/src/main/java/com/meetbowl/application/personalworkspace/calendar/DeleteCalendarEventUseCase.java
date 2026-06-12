package com.meetbowl.application.personalworkspace.calendar;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/**
 * 개인 일정을 삭제한다.
 *
 * <p>삭제 port는 소유자이면서 PERSONAL 출처인 일정만 지운다. 회의 투영 일정은 삭제 대상에서 제외되므로 결과가 0건이면 존재하지 않거나 삭제 불가한 일정으로 보고
 * NOT_FOUND를 반환한다.
 */
@Service
public class DeleteCalendarEventUseCase {

    private final PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort;

    public DeleteCalendarEventUseCase(
            PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort) {
        this.calendarEventRepositoryPort = calendarEventRepositoryPort;
    }

    @Transactional
    public void execute(UUID userId, UUID eventId) {
        boolean deleted =
                calendarEventRepositoryPort.deletePersonalByIdAndOwnerUserId(eventId, userId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "삭제할 개인 일정을 찾을 수 없습니다.");
        }
    }
}
