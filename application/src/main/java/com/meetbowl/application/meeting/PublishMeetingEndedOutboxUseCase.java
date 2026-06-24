package com.meetbowl.application.meeting;

import java.time.Clock;
import java.time.Instant;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.domain.meeting.MeetingEndedEventOutboxPort;
import com.meetbowl.domain.meeting.MeetingEndedEventPublisher;

/**
 * 미발행 meeting.ended Outbox를 RabbitMQ로 전달한다.
 *
 * <p>행 잠금을 잡은 트랜잭션 안에서 broker confirm까지 확인한 뒤 완료 처리한다. confirm 후 DB 갱신 전에 프로세스가 중단되면 같은 eventId로
 * 재발행될 수 있으며, 이 경우 consumer의 eventId 멱등 처리가 중복 생성을 막는다.
 */
@Service
public class PublishMeetingEndedOutboxUseCase {

    private static final Logger log =
            Logger.getLogger(PublishMeetingEndedOutboxUseCase.class.getName());
    private static final int BATCH_SIZE = 20;
    private static final long MAX_RETRY_DELAY_SECONDS = 300L;

    private final MeetingEndedEventOutboxPort outboxPort;
    private final MeetingEndedEventPublisher eventPublisher;
    private final Clock clock;

    public PublishMeetingEndedOutboxUseCase(
            MeetingEndedEventOutboxPort outboxPort,
            MeetingEndedEventPublisher eventPublisher,
            Clock clock) {
        this.outboxPort = outboxPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public int run() {
        Instant now = clock.instant();
        int publishedCount = 0;
        for (MeetingEndedEvent event : outboxPort.findReadyToPublish(now, BATCH_SIZE)) {
            try {
                eventPublisher.publish(event);
                outboxPort.removePublished(event.eventId());
                publishedCount++;
            } catch (RuntimeException exception) {
                Instant nextAttemptAt =
                        now.plusSeconds(retryDelaySeconds(event.publishAttempts() + 1));
                outboxPort.markFailed(event.eventId(), nextAttemptAt, safeFailureReason(exception));
                log.warning(
                        "meeting.ended Outbox 발행에 실패했습니다. eventId="
                                + event.eventId()
                                + ", nextAttemptAt="
                                + nextAttemptAt
                                + ", message="
                                + exception.getMessage());
            }
        }
        return publishedCount;
    }

    private long retryDelaySeconds(int attempt) {
        long exponentialDelay = 1L << Math.min(Math.max(attempt - 1, 0), 8);
        return Math.min(exponentialDelay, MAX_RETRY_DELAY_SECONDS);
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
