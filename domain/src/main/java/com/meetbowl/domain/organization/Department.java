package com.meetbowl.domain.organization;

import java.time.Instant;
import java.util.UUID;

public record Department(
        UUID id,
        UUID organizationId,
        UUID parentDepartmentId,
        String name,
        String code,
        ReferenceStatus status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public Department {
        Organization.validate(name, status);
    }

    public boolean isActive() {
        return status == ReferenceStatus.ACTIVE;
    }
}
