package com.meetbowl.application.personalworkspace.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupSourceType;

/** 백업 자료 목록의 북마크 표시, 북마크 소유자 검증, 멱등 처리 규칙을 검증한다. */
class BackupUseCaseTest {

    private final PersonalWorkspaceBackupRepositoryPort backupPort =
            Mockito.mock(PersonalWorkspaceBackupRepositoryPort.class);
    private final PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkPort =
            Mockito.mock(PersonalWorkspaceBackupBookmarkRepositoryPort.class);

    private PersonalWorkspaceBackup backupOf(UUID id, UUID ownerUserId) {
        return PersonalWorkspaceBackup.of(
                id,
                ownerUserId,
                PersonalWorkspaceBackupSourceType.MAIL,
                UUID.randomUUID(),
                "분기 보고 메일 백업",
                null,
                Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test
    void getBackups_marks_bookmarked_flag() {
        GetBackupsUseCase useCase = new GetBackupsUseCase(backupPort, bookmarkPort);
        UUID userId = UUID.randomUUID();
        UUID bookmarkedBackupId = UUID.randomUUID();
        UUID plainBackupId = UUID.randomUUID();
        when(backupPort.findByOwnerUserId(userId))
                .thenReturn(
                        List.of(
                                backupOf(bookmarkedBackupId, userId),
                                backupOf(plainBackupId, userId)));
        when(bookmarkPort.findByOwnerUserId(userId))
                .thenReturn(
                        List.of(
                                PersonalWorkspaceBackupBookmark.create(
                                        userId, bookmarkedBackupId, Instant.now())));

        List<BackupResult> results = useCase.execute(userId);

        assertEquals(2, results.size());
        assertTrue(findById(results, bookmarkedBackupId).bookmarked());
        assertFalse(findById(results, plainBackupId).bookmarked());
    }

    @Test
    void addBookmark_fail_when_not_owner() {
        AddBackupBookmarkUseCase useCase = new AddBackupBookmarkUseCase(backupPort, bookmarkPort);
        UUID userId = UUID.randomUUID();
        UUID backupId = UUID.randomUUID();
        when(backupPort.findById(backupId))
                .thenReturn(Optional.of(backupOf(backupId, UUID.randomUUID())));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> useCase.execute(userId, backupId));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, ex.errorCode());
        verify(bookmarkPort, never()).save(any());
    }

    @Test
    void addBookmark_is_idempotent_when_already_bookmarked() {
        AddBackupBookmarkUseCase useCase = new AddBackupBookmarkUseCase(backupPort, bookmarkPort);
        UUID userId = UUID.randomUUID();
        UUID backupId = UUID.randomUUID();
        when(backupPort.findById(backupId)).thenReturn(Optional.of(backupOf(backupId, userId)));
        when(bookmarkPort.findByOwnerUserIdAndBackupId(userId, backupId))
                .thenReturn(
                        Optional.of(
                                PersonalWorkspaceBackupBookmark.create(
                                        userId, backupId, Instant.now())));

        useCase.execute(userId, backupId);

        verify(bookmarkPort, never()).save(any());
    }

    private BackupResult findById(List<BackupResult> results, UUID backupId) {
        return results.stream()
                .filter(result -> result.backupId().equals(backupId))
                .findFirst()
                .orElseThrow();
    }
}
