package com.meetbowl.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 서버 간 RabbitMQ/Redis Stream 이벤트의 공통 Envelope다.
 * 필드 변경이 필요하면 루트 docs/event-contract.md를 먼저 갱신한다.
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        int version,
        UUID correlationId,
        T payload
) {
}
