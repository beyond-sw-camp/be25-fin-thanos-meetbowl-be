package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;

/** 메일을 개인 워크스페이스 백업으로 등록한 결과다. 생성된 백업과 원본 메일을 연결해 돌려준다. */
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
