package com.meetbowl.application.minutes;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 지정 검토자가 승인 전 회의록 요약과 본문을 수정하고 검토 중 상태로 전환하는 UseCase다. */
@Service
public class ReviseMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;

    public ReviseMinutesUseCase(MinutesRepositoryPort minutesRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
    }

    @Transactional
    public MinutesResult execute(ReviseMinutesCommand command) {
        Minutes minutes = findByMeetingId(command.meetingId());

        // 회원·회의 테이블이 아직 없으므로 JWT 조직을 1차 테넌트 경계로 사용한다.
        // 지정 검토자 여부와 승인 이후 수정 금지는 이어지는 도메인 메서드가 판정한다.
        MinutesAccessValidator.ensureSameOrganization(minutes, command.actorOrganizationId());

        // 수정과 IN_REVIEW 상태 전이를 한 트랜잭션에서 저장해 수정 내용만 반영되는 상태를 방지한다.
        Minutes revised = minutes.revise(command.summary(), command.content(), command.actorUserId());
        return MinutesResult.from(minutesRepositoryPort.save(revised));
    }

    /** 회의 테이블이 없는 현재 단계에서는 회의록의 meetingId unique 관계를 조회 기준으로 사용한다. */
    private Minutes findByMeetingId(UUID meetingId) {
        return minutesRepositoryPort
                .findByMeetingId(meetingId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
    }
}
