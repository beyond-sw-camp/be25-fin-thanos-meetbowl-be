package com.meetbowl.infrastructure.messaging.meeting;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** Outbox 이벤트가 eventId와 occurredAt을 보존한 채 계약 Message DTO로 변환되는지 검증한다. */
class RabbitMeetingEndedEventPublisherTest {

    @Test
    void publishMeetingEndedMessageWithStableEnvelopeIdentity() {
        RabbitEventPublisher commonPublisher = mock(RabbitEventPublisher.class);
        RabbitMeetingEndedEventPublisher publisher =
                new RabbitMeetingEndedEventPublisher(commonPublisher);
        MeetingEndedEvent event =
                new MeetingEndedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "주간 전략 회의",
                        Instant.parse("2099-01-01T01:00:00Z"),
                        Instant.parse("2099-01-01T02:00:00Z"),
                        Instant.parse("2099-01-01T02:00:01Z"),
                        0);

        publisher.publish(event);

        verify(commonPublisher)
                .publish(
                        eq(EventTypes.MEETING_ENDED),
                        argThat(
                                payload -> {
                                    MeetingEndedMessage message = (MeetingEndedMessage) payload;
                                    return message.meetingId().equals(event.meetingId())
                                            && message.organizationId()
                                                    .equals(event.organizationId())
                                            && message.hostUserId().equals(event.hostUserId())
                                            && message.reviewerUserId()
                                                    .equals(event.reviewerUserId())
                                            && message.title().equals(event.title())
                                            && message.startedAt().equals(event.startedAt())
                                            && message.endedAt().equals(event.endedAt());
                                }),
                        eq(event.eventId()),
                        eq(event.correlationId()),
                        eq(event.occurredAt()));
    }
}
