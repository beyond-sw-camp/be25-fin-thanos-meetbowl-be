package com.meetbowl.application.minutes;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;

/** 지정 검토자가 회의록을 최종 승인하고 AI 검색용 문서 색인을 요청하는 UseCase다. */
@Service
public class ApproveMinutesUseCase {

    private static final String MEETING_MINUTES_DOCUMENT_TYPE = "MEETING_MINUTES";
    private static final String TEMPORARY_MINUTES_TITLE = "회의록";

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;
    private final Clock clock;

    /**
     * 운영과 테스트 모두 동일한 생성자 계약을 사용한다.
     *
     * <p>운영에서는 app-api의 UTC Clock Bean을, 테스트에서는 고정 Clock을 주입해 승인 시각을 결정적으로 검증한다.
     */
    public ApproveMinutesUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort,
            Clock clock) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
        this.clock = clock;
    }

    @Transactional
    public MinutesResult execute(ApproveMinutesCommand command) {
        Minutes minutes = findByMeetingId(command.meetingId());

        // 조직 경계를 먼저 확인한 뒤 도메인에서 지정 검토자 일치와 현재 상태를 검증한다.
        // 승인 저장과 이벤트 발행을 같은 UseCase 흐름에서 처리해 발행 실패 시 승인 요청도 실패로 반환한다.
        MinutesAccessValidator.ensureSameOrganization(minutes, command.actorOrganizationId());

        // 클라이언트 시각은 신뢰하지 않고 서버 UTC Clock으로 승인 시각을 확정한다.
        Minutes approved = minutes.approve(command.actorUserId(), Instant.now(clock));
        Minutes saved = minutesRepositoryPort.save(approved);

        // 현재 회의/참석자 테이블이 없으므로 제목은 임시값을 쓰고, 접근 범위는 검토자 본인으로 제한한다.
        // 향후 회의 메타데이터와 참여자 조회 Port가 생기면 실제 제목과 열람 가능 사용자 범위로 확장한다.
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        saved.id(),
                        MEETING_MINUTES_DOCUMENT_TYPE,
                        saved.organizationId(),
                        saved.reviewerUserId(),
                        TEMPORARY_MINUTES_TITLE,
                        saved.summary(),
                        List.of(saved.reviewerUserId()),
                        List.of(),
                        List.of()));
        return MinutesResult.from(saved);
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
