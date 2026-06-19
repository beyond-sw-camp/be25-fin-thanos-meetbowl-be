package com.meetbowl.api.admin.dto;

import com.meetbowl.application.admin.AdminUserSearchIndexUseCase;

public record AdminUserSearchReindexResponse(long processedCount, long failedCount) {

    public static AdminUserSearchReindexResponse from(
            AdminUserSearchIndexUseCase.ReindexResult result) {
        return new AdminUserSearchReindexResponse(result.processedCount(), result.failedCount());
    }
}
