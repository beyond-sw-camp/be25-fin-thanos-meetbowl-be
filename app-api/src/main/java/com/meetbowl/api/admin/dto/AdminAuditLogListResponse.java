package com.meetbowl.api.admin.dto;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.AdminAuditLogPageResult;

public record AdminAuditLogListResponse(
        List<AdminAuditLogResponse> items, int page, int size, long totalElements, int totalPages) {

    public static AdminAuditLogListResponse from(
            AdminAuditLogPageResult result, ObjectMapper objectMapper) {
        return new AdminAuditLogListResponse(
                result.items().stream()
                        .map(item -> AdminAuditLogResponse.from(item, objectMapper))
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
