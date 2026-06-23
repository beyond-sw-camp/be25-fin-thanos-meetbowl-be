package com.meetbowl.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPositionRequest(
        @NotBlank String name,
        String code,
        @NotNull AdminOrganizationReferenceStatus status,
        Integer sortOrder) {}
