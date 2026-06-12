package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 회원 수정 요청 DTO
 */
public record AdminUserUpdateRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull AdminUserRole role,
        UUID affiliateId,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        Instant activeFrom,
        Instant activeUntil) {}
