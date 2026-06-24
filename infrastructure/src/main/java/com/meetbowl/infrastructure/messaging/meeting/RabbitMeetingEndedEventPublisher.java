package com.meetbowl.infrastructure.messaging.meeting;

import org.springframework.stereotype.Component;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.domain.meeting.MeetingEndedEvent;
import com.meetbowl.domain.meeting.MeetingEndedEventPublisher;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** Outbox의 meeting.ended 이벤트를 계약 Message DTO와 공통 Envelope로 발행한다. */
@Component
public class RabbitMeetingEndedEventPublisher implements MeetingEndedEventPublisher {

    private final RabbitEventPublisher rabbitEventPublisher;

    public RabbitMeetingEndedEventPublisher(RabbitEventPublisher rabbitEventPublisher) {
        this.rabbitEventPublisher = rabbitEventPublisher;
    }

    @Override
    public void publish(MeetingEndedEvent event) {
        rabbitEventPublisher.publish(
                EventTypes.MEETING_ENDED,
                new MeetingEndedMessage(
                        event.meetingId(),
                        event.organizationId(),
                        event.hostUserId(),
                        event.reviewerUserId(),
                        event.title(),
                        event.startedAt(),
                        event.endedAt()),
                event.eventId(),
                event.correlationId(),
                event.occurredAt());
    }
}
