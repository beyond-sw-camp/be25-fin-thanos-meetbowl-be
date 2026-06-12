package com.meetbowl.application.personalworkspace.calendar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceCalendarEventRepositoryPort;

/**
 * 조회 기간 내에 사용자가 볼 수 있는 일정을 모두 반환한다.
 *
 * <p>본인이 만든 개인 일정과 회의 투영 일정뿐 아니라 구독한 동료의 일정까지 함께 보여줘야 하므로 소유자 기준 조회가 아니라 가시성 기준 조회를 사용한다.
 */
@Service
public class GetCalendarUseCase {

    private final PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort;

    public GetCalendarUseCase(
            PersonalWorkspaceCalendarEventRepositoryPort calendarEventRepositoryPort) {
        this.calendarEventRepositoryPort = calendarEventRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResult> execute(UUID userId, Instant from, Instant to) {
        // 조회 구간이 뒤집히면 빈 결과가 아니라 잘못된 요청으로 처리해 호출 측 버그를 빨리 드러낸다.
        if (from == null || to == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "조회 시작/종료 시각은 필수입니다.");
        }
        if (!from.isBefore(to)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "조회 시작 시각은 종료 시각보다 이전이어야 합니다.");
        }

        return calendarEventRepositoryPort.findVisibleByUserIdAndPeriod(userId, from, to).stream()
                .map(CalendarEventResult::from)
                .toList();
    }
}
