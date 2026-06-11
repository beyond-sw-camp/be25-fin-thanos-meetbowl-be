package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminUserUpdateRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        UUID affiliateId,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        Instant activeFrom,
        Instant activeUntil) {}
