package com.meetbowl.application.minutes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.application.notification.DispatchNotificationCommand;
import com.meetbowl.application.notification.DispatchNotificationUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesGeneratedEventRepositoryPort;
import com.meetbowl.domain.minutes.MinutesRepositoryPort;
import com.meetbowl.domain.minutes.MinutesStatus;
import com.meetbowl.domain.notification.NotificationResourceType;
import com.meetbowl.domain.notification.NotificationType;

/** AI 서버가 발행한 회의록 초안을 저장하거나 재생성 결과로 교체한다. */
@Service
public class SyncGeneratedMinutesUseCase {

    private final MinutesRepositoryPort minutesRepositoryPort;
    private final DispatchNotificationUseCase dispatchNotificationUseCase;
    private final MinutesGeneratedEventRepositoryPort eventRepositoryPort;

    public SyncGeneratedMinutesUseCase(
            MinutesRepositoryPort minutesRepositoryPort,
            DispatchNotificationUseCase dispatchNotificationUseCase,
            MinutesGeneratedEventRepositoryPort eventRepositoryPort) {
        this.minutesRepositoryPort = minutesRepositoryPort;
        this.dispatchNotificationUseCase = dispatchNotificationUseCase;
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
        // AI 1차 요약 완료 = 검토 대기 시작. 검토 요청(IN_REVIEW)으로 전환하고, 멱등용 inbox 기록 후 지정 검토자에게 알린다.
        // IN_REVIEW로 둬야 검토 미완료 재알림(MINUTES_REVIEW_REMINDER) 스케줄러의 대상이 된다.
        Minutes saved = minutesRepositoryPort.save(draft.requestReview());
        // 회의록 저장과 inbox 기록을 같은 트랜잭션에 묶어 한쪽만 반영되는 상태를 막는다.
        eventRepositoryPort.save(command.eventId(), command.meetingId());
        notifyReviewRequested(saved);
        return MinutesResult.from(saved);
    }

    /**
     * AI 초안 준비 완료를 지정 검토자에게 알린다(MINUTES_REVIEW_REQUEST).
     *
     * <p>이 알림 행의 createdAt이 검토 미완료 재알림의 기준 시각(검토 요청 시각) 원장으로도 쓰인다({@link
     * com.meetbowl.application.notification.SendMinutesReviewRemindersUseCase}). 발송은 현재 트랜잭션에 함께 저장되고
     * 커밋 후 SSE로 전달된다(초안 저장이 롤백되면 알림도 함께 롤백).
     */
    private void notifyReviewRequested(Minutes minutes) {
        dispatchNotificationUseCase.execute(
                new DispatchNotificationCommand(
                        minutes.reviewerUserId(),
                        NotificationType.MINUTES_REVIEW_REQUEST.name(),
                        "회의록 검토 요청",
                        "AI가 작성한 회의록 초안이 준비되었습니다. 검토를 진행해 주세요.",
                        NotificationResourceType.MEETING_MINUTES.name(),
                        minutes.id()));
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
