package com.meetbowl.api.admin.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminTeamRequest(
        @NotNull UUID departmentId,
        @NotBlank String name,
        @NotBlank String code,
        @NotNull AdminOrganizationReferenceStatus status,
        Integer sortOrder) {}
