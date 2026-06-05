package com.meetbowl.common.event;

import java.time.Instant;
import java.util.UUID;

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
