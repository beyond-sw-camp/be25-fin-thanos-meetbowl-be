package com.meetbowl.api.admin.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPositionRequest(
        UUID affiliateId,
        @NotBlank String name,
        String code,
        @NotNull AdminOrganizationReferenceStatus status,
        Integer sortOrder) {}
