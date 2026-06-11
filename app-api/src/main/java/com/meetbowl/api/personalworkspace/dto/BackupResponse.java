package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.backup.BackupResult;

/** 백업 자료 응답 DTO다. bookmarked로 화면의 북마크 상태를 표시한다. */
public record BackupResponse(
        UUID backupId,
        UUID ownerUserId,
        String sourceType,
        UUID sourceId,
        String title,
        String summary,
        Instant backedUpAt,
        boolean bookmarked) {

    public static BackupResponse from(BackupResult result) {
        return new BackupResponse(
                result.backupId(),
                result.ownerUserId(),
                result.sourceType(),
                result.sourceId(),
                result.title(),
                result.summary(),
                result.backedUpAt(),
                result.bookmarked());
    }
}
