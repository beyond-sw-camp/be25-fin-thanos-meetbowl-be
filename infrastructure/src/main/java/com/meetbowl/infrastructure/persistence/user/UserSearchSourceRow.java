package com.meetbowl.infrastructure.persistence.user;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

public record UserSearchSourceRow(
        UUID userId,
        String loginId,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        UUID affiliateId,
        String affiliateName,
        UUID departmentId,
        String departmentName,
        UUID teamId,
        String teamName,
        UUID positionId,
        String positionName,
        Instant activeFrom,
        Instant activeUntil,
        Instant deletedAt,
        Instant createdAt) {}
