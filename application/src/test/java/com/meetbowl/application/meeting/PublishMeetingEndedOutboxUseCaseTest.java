package com.meetbowl.application.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.domain.meeting.MeetingEndedEventOutboxPort;
import com.meetbowl.domain.meeting.MeetingEndedEventPublisher;

class PublishMeetingEndedOutboxUseCaseTest {

    private static final Instant NOW = Instant.parse("2099-01-01T02:00:00Z");

    @Test
    void brokerConfirm성공후Outbox를완료처리한다() {
        RecordingOutbox outbox = new RecordingOutbox(event(0));
        RecordingPublisher publisher = new RecordingPublisher(false);
        PublishMeetingEndedOutboxUseCase useCase =
                new PublishMeetingEndedOutboxUseCase(
                        outbox, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

        int publishedCount = useCase.run();

        assertEquals(1, publishedCount);
        assertEquals(outbox.events.getFirst().eventId(), outbox.publishedEventId);
        assertEquals(outbox.events.getFirst().eventId(), publisher.publishedEventId);
    }

    @Test
    void 발행실패를기록하고지수백오프로재시도예약한다() {
        RecordingOutbox outbox = new RecordingOutbox(event(2));
        PublishMeetingEndedOutboxUseCase useCase =
                new PublishMeetingEndedOutboxUseCase(
                        outbox, new RecordingPublisher(true), Clock.fixed(NOW, ZoneOffset.UTC));

        int publishedCount = useCase.run();

        assertEquals(0, publishedCount);
        assertEquals(NOW.plusSeconds(4), outbox.nextAttemptAt);
        assertEquals("rabbitmq unavailable", outbox.failureReason);
    }

    private MeetingEndedEvent event(int attempts) {
        return new MeetingEndedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "주간 전략 회의",
                Instant.parse("2099-01-01T01:00:00Z"),
                NOW,
                NOW,
                attempts);
    }

    private static final class RecordingPublisher implements MeetingEndedEventPublisher {
        private final boolean fail;
        private UUID publishedEventId;

        private RecordingPublisher(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void publish(MeetingEndedEvent event) {
            if (fail) {
                throw new IllegalStateException("rabbitmq unavailable");
            }
            publishedEventId = event.eventId();
        }
    }

    private static final class RecordingOutbox implements MeetingEndedEventOutboxPort {
        private final List<MeetingEndedEvent> events = new ArrayList<>();
        private UUID publishedEventId;
        private Instant nextAttemptAt;
        private String failureReason;

        private RecordingOutbox(MeetingEndedEvent event) {
            events.add(event);
        }

        @Override
        public void save(MeetingEndedEvent event) {
            events.add(event);
        }

        @Override
        public List<MeetingEndedEvent> findReadyToPublish(Instant now, int limit) {
            return events.stream().limit(limit).toList();
        }

        @Override
        public void removePublished(UUID eventId) {
            this.publishedEventId = eventId;
        }

        @Override
        public void markFailed(UUID eventId, Instant nextAttemptAt, String failureReason) {
            this.nextAttemptAt = nextAttemptAt;
            this.failureReason = failureReason;
        }
    }
}
