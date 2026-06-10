package com.meetbowl.domain.document;

/** application이 문서 색인 요청 발행을 위임하는 Port다. RabbitMQ 같은 전송 기술은 infrastructure가 구현한다. */
public interface DocumentIndexRequestedEventPort {

    void publish(DocumentIndexRequestedEvent event);
}
