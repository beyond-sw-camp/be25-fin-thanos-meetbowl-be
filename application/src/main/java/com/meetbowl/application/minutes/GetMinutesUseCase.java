package com.meetbowl.application.minutes;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 회의별 회의록 상세를 조회한다. 현재 단계에서는 조직 경계만 먼저 검증한다. */
@Service
public class GetMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;

    public GetMinutesUseCase(MinutesRepositoryPort minutesRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
    }

    @Transactional(readOnly = true)
    public MinutesResult get(UUID meetingId, UUID actorOrganizationId) {
        Minutes minutes =
                minutesRepositoryPort
                        .findByMeetingId(meetingId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.MINUTES_NOT_FOUND,
                                                "회의록을 찾을 수 없습니다."));
        MinutesAccessValidator.ensureSameOrganization(minutes, actorOrganizationId);
        return MinutesResult.from(minutes);
    }
}
