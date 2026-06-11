package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;

public record BackupMailResult(
        UUID backupId, UUID mailId, String title, String summary, Instant backedUpAt) {

    static BackupMailResult from(PersonalWorkspaceBackup backup) {
        return new BackupMailResult(
                backup.id(),
                backup.sourceId(),
                backup.title(),
                backup.summary(),
                backup.backedUpAt());
    }
}
