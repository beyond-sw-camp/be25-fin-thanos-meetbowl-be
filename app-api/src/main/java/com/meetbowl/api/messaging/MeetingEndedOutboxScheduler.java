package com.meetbowl.api.messaging;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.meetbowl.application.meeting.PublishMeetingEndedOutboxUseCase;

/** DB에 저장된 meeting.ended Outbox를 짧은 주기로 RabbitMQ에 전달한다. */
@Component
public class MeetingEndedOutboxScheduler {

    private final PublishMeetingEndedOutboxUseCase publishUseCase;

    public MeetingEndedOutboxScheduler(PublishMeetingEndedOutboxUseCase publishUseCase) {
        this.publishUseCase = publishUseCase;
    }

    @Scheduled(
            fixedDelayString = "${meetbowl.rabbitmq.outbox.fixed-delay-millis:1000}",
            initialDelayString = "${meetbowl.rabbitmq.outbox.initial-delay-millis:1000}")
    public void publishPendingEvents() {
        publishUseCase.run();
    }
}
