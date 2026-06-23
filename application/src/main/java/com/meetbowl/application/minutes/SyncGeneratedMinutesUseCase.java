package com.meetbowl.application.minutes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesGeneratedEventRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;

/** AI 서버가 발행한 회의록 초안을 저장하거나 재생성 결과로 교체한다. */
@Service
public class SyncGeneratedMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final MinutesGeneratedEventRepositoryPort eventRepositoryPort;

    public SyncGeneratedMinutesUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            MinutesGeneratedEventRepositoryPort eventRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.eventRepositoryPort = eventRepositoryPort;
    }

    @Transactional
    public MinutesResult execute(SyncGeneratedMinutesCommand command) {
        // RabbitMQ 재전달은 같은 eventId를 유지하므로, 이미 완료한 이벤트는 현재 저장 결과만 반환한다.
        if (eventRepositoryPort.existsByEventId(command.eventId())) {
            return MinutesResult.from(findByMeetingId(command.meetingId()));
        }
        Minutes draft =
                minutesRepositoryPort
                        .findByMeetingId(command.meetingId())
                        .map(existing -> replaceDraft(existing, command))
                        .orElseGet(
                                () ->
                                        Minutes.createDraft(
                                                command.meetingId(),
                                                command.organizationId(),
                                                command.reviewerUserId(),
                                                command.summary(),
                                                command.content(),
                                                command.model(),
                                                command.promptVersion()));
        Minutes saved = minutesRepositoryPort.save(draft);
        // 회의록 저장과 inbox 기록을 같은 트랜잭션에 묶어 한쪽만 반영되는 상태를 막는다.
        eventRepositoryPort.save(command.eventId(), command.meetingId());
        return MinutesResult.from(saved);
    }

    private Minutes replaceDraft(Minutes existing, SyncGeneratedMinutesCommand command) {
        if (!existing.organizationId().equals(command.organizationId())) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "같은 회의 ID로 다른 조직의 회의록 초안을 저장할 수 없습니다.");
        }
        if (existing.status() != MinutesStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "검토가 시작된 회의록은 AI 초안으로 덮어쓸 수 없습니다.");
        }
        return Minutes.of(
                existing.id(),
                command.meetingId(),
                command.organizationId(),
                command.reviewerUserId(),
                MinutesStatus.DRAFT,
                command.summary(),
                command.content(),
                command.model(),
                command.promptVersion(),
                null,
                null,
                null);
    }

    private Minutes findByMeetingId(java.util.UUID meetingId) {
        return minutesRepositoryPort
                .findByMeetingId(meetingId)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.MINUTES_NOT_FOUND, "회의록을 찾을 수 없습니다."));
    }
}
