package com.meetbowl.application.personalworkspace.backup;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;

/**
 * 백업 자료 UseCase의 출력 모델이다.
 *
 * <p>원본 유형(sourceType)은 도메인 enum 대신 문자열로 노출한다. 목록/검색 화면에서 북마크 버튼 상태를 그릴 수 있도록 사용자의 북마크 여부를 함께 담는다.
 */
public record BackupResult(
        UUID backupId,
        UUID ownerUserId,
        String sourceType,
        UUID sourceId,
        String title,
        String summary,
        Instant backedUpAt,
        boolean bookmarked) {

    public static BackupResult from(PersonalWorkspaceBackup backup, boolean bookmarked) {
        return new BackupResult(
                backup.id(),
                backup.ownerUserId(),
                backup.sourceType().name(),
                backup.sourceId(),
                backup.title(),
                backup.summary(),
                backup.backedUpAt(),
                bookmarked);
    }
}
