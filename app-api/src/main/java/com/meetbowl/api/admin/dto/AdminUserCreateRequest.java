package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 관리자 회원 생성 요청 DTO */
public record AdminUserCreateRequest(
        @NotBlank String loginId,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull AdminUserRole role,
        @NotNull AdminUserStatus status,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        Instant activeFrom,
        Instant activeUntil) {}
