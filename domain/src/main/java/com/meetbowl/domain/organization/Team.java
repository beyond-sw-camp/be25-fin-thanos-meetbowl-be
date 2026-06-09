package com.meetbowl.domain.organization;

import java.time.Instant;
import java.util.UUID;

public record Team(
        UUID id,
        UUID departmentId,
        String name,
        String code,
        ReferenceStatus status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public Team {
        Affiliate.validate(name, status);
    }

    public boolean isActive() {
        return status == ReferenceStatus.ACTIVE;
    }
}
