package com.meetbowl.domain.organization;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record Organization(
        UUID id,
        String name,
        String code,
        ReferenceStatus status,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public Organization {
        validate(name, status);
    }

    static void validate(String name, ReferenceStatus status) {
        if (name == null || name.isBlank() || status == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "기준정보 이름과 상태는 필수입니다.");
        }
    }

    public boolean isActive() {
        return status == ReferenceStatus.ACTIVE;
    }
}
