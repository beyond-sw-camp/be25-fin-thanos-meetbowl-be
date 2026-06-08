package com.meetbowl.domain.organization;

import java.time.Instant;
import java.util.UUID;

public record Job(
        UUID id,
        String name,
        String code,
        ReferenceStatus status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public Job {
        Organization.validate(name, status);
    }

    public boolean isActive() {
        return status == ReferenceStatus.ACTIVE;
    }
}
