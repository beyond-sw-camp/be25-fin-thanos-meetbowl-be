package com.meetbowl.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ와 Redis Stream 소비자가 전송 수단과 무관하게 같은 추적·버전 정보를 신뢰하도록 이벤트 외피를 고정한다.
 * 이 타입의 변경은 여러 서버의 소비 계약을 동시에 깨뜨릴 수 있으므로 개별 기능에서 임의로 확장하지 않는다.
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        int version,
        UUID correlationId,
        T payload) {}
