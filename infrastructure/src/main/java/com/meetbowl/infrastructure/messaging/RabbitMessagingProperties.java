package com.meetbowl.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** BE가 발행하는 일반 도메인 이벤트의 공통 RabbitMQ 설정이다. routing key는 이벤트 계약 상수를 사용한다. */
@ConfigurationProperties(prefix = "meetbowl.rabbitmq")
public record RabbitMessagingProperties(
        String exchange, String producer, int eventVersion, long confirmTimeoutMillis) {}
