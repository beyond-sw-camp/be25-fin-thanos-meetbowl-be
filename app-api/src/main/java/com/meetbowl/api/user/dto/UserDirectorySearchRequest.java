package com.meetbowl.api.user.dto;

import java.util.UUID;

public record UserDirectorySearchRequest(
        String keyword,
        UUID affiliateId,
        UUID departmentId,
        UUID teamId,
        UUID positionId,
        String status,
        Integer page,
        Integer size) {}
