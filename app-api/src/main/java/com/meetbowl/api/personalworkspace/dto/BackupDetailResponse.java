package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.backup.BackupDetailResult;

/** 개인 워크스페이스 백업 상세 응답이다. */
public record BackupDetailResponse(
        UUID backupId,
        String sourceType,
        UUID sourceId,
        String title,
        String summary,
        String body,
        Instant backedUpAt) {

    public static BackupDetailResponse from(BackupDetailResult result) {
        return new BackupDetailResponse(
                result.backupId(),
                result.sourceType(),
                result.sourceId(),
                result.title(),
                result.summary(),
                result.body(),
                result.backedUpAt());
    }
}
