package com.meetbowl.api.mail.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.mail.BackupMailResult;

/** 메일 백업 결과 응답 본문이다. 생성된 백업과 원본 메일 연결 정보를 클라이언트 계약으로 노출한다. */
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
