package com.meetbowl.application.personalworkspace.backup;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;

/** 개인 워크스페이스 백업 상세 조회 결과다. */
public record BackupDetailResult(
        UUID backupId,
        String sourceType,
        UUID sourceId,
        String title,
        String summary,
        String body,
        Instant backedUpAt) {

    public static BackupDetailResult from(PersonalWorkspaceBackup backup) {
        return new BackupDetailResult(
                backup.id(),
                backup.sourceType().name(),
                backup.sourceId(),
                backup.title(),
                backup.summary(),
                backup.body(),
                backup.backedUpAt());
    }
}
