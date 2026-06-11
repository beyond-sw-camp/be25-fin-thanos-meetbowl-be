package com.meetbowl.infrastructure.messaging.mail;

import org.springframework.stereotype.Component;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.domain.mail.MailDeliveryEventPort;
import com.meetbowl.domain.mail.MailFailedEvent;
import com.meetbowl.domain.mail.MailSentEvent;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** 내부 메일 발송 결과 도메인 이벤트를 계약 Message DTO로 변환해 공통 RabbitMQ 발행기에 전달한다. */
@Component
public class RabbitMailDeliveryEventPublisher implements MailDeliveryEventPort {

    private final RabbitEventPublisher rabbitEventPublisher;

    public RabbitMailDeliveryEventPublisher(RabbitEventPublisher rabbitEventPublisher) {
        this.rabbitEventPublisher = rabbitEventPublisher;
    }

    @Override
    public void publishSent(MailSentEvent event) {
        // 이벤트 이름을 routing key로 사용하고, 공통 발행기가 Envelope 래핑과 영속 전송을 처리한다.
        rabbitEventPublisher.publish(EventTypes.MAIL_SENT, MailSentMessage.from(event));
    }

    @Override
    public void publishFailed(MailFailedEvent event) {
        rabbitEventPublisher.publish(EventTypes.MAIL_FAILED, MailFailedMessage.from(event));
    }
}
