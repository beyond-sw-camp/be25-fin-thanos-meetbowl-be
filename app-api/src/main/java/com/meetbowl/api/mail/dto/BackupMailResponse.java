package com.meetbowl.api.mail.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.mail.BackupMailResult;

public record BackupMailResponse(
        UUID backupId, UUID mailId, String title, String summary, Instant backedUpAt) {

    public static BackupMailResponse from(BackupMailResult result) {
        return new BackupMailResponse(
                result.backupId(),
                result.mailId(),
                result.title(),
                result.summary(),
                result.backedUpAt());
    }
}
