package com.meetbowl.infrastructure.messaging.document;

import org.springframework.stereotype.Component;

import com.meetbowl.common.event.EventTypes;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.infrastructure.messaging.RabbitEventPublisher;

/** 문서 색인 도메인 이벤트를 계약 Message DTO로 변환해 공통 RabbitMQ 발행기에 전달한다. */
@Component
public class RabbitDocumentIndexRequestedEventPublisher implements DocumentIndexRequestedEventPort {

    private final RabbitEventPublisher rabbitEventPublisher;

    public RabbitDocumentIndexRequestedEventPublisher(RabbitEventPublisher rabbitEventPublisher) {
        this.rabbitEventPublisher = rabbitEventPublisher;
    }

    @Override
    public void publish(DocumentIndexRequestedEvent event) {
        rabbitEventPublisher.publish(
                EventTypes.DOCUMENT_INDEX_REQUESTED, DocumentIndexRequestedMessage.from(event));
    }
}
