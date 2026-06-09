package com.meetbowl.application.minutes;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 지정 검토자가 회의록을 최종 승인하는 UseCase다. 이벤트 발행과 공유는 후속 구현으로 분리한다. */
@Service
public class ApproveMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final Clock clock;

    /**
     * 운영과 테스트 모두 동일한 생성자 계약을 사용한다.
     *
     * <p>운영에서는 app-api의 UTC Clock Bean을, 테스트에서는 고정 Clock을 주입해 승인 시각을 결정적으로 검증한다.
     */
    public ApproveMinutesUseCase(MinutesRepositoryPort minutesRepositoryPort, Clock clock) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public MinutesResult execute(ApproveMinutesCommand command) {
        Minutes minutes = findByMeetingId(command.meetingId());

        // 조직 경계를 먼저 확인한 뒤 도메인에서 지정 검토자 일치와 현재 상태를 검증한다.
        // 이벤트 발행 전 단계이므로 이 트랜잭션은 APPROVED 상태와 승인 시각 저장까지만 책임진다.
        MinutesAccessValidator.ensureSameOrganization(minutes, command.actorOrganizationId());

        // 클라이언트 시각은 신뢰하지 않고 서버 UTC Clock으로 승인 시각을 확정한다.
        Minutes approved = minutes.approve(command.actorUserId(), Instant.now(clock));
        return MinutesResult.from(minutesRepositoryPort.save(approved));
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
