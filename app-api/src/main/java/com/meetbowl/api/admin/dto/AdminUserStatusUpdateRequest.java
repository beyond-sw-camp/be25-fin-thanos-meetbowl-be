package com.meetbowl.api.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(@NotNull AdminUserStatus status) {}
