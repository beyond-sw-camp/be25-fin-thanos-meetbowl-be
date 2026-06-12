package com.meetbowl.api.admin.dto;

import jakarta.validation.constraints.NotNull;

/** 관리자 회원 상태 수정 요청 DTO */
public record AdminUserStatusUpdateRequest(@NotNull AdminUserStatus status) {}
