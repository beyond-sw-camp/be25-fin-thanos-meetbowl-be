package com.meetbowl.application.personalworkspace.backup;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

/**
 * 사용자의 백업 자료 목록을 조회한다.
 *
 * <p>백업과 북마크는 별도 저장소이므로 북마크 식별자를 한 번에 조회해 메모리에서 합친다. 목록 건마다 북마크 존재 여부를 질의하면 N+1이 되므로 사용자 북마크를 Set으로
 * 모아 O(1)로 판정한다.
 */
@Service
public class GetBackupsUseCase {

    private final PersonalWorkspaceBackupRepositoryPort backupRepositoryPort;
    private final PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort;

    public GetBackupsUseCase(
            PersonalWorkspaceBackupRepositoryPort backupRepositoryPort,
            PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort) {
        this.backupRepositoryPort = backupRepositoryPort;
        this.bookmarkRepositoryPort = bookmarkRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<BackupResult> execute(UUID userId) {
        Set<UUID> bookmarkedBackupIds = loadBookmarkedBackupIds(userId);
        return backupRepositoryPort.findByOwnerUserId(userId).stream()
                .map(backup -> BackupResult.from(backup, bookmarkedBackupIds.contains(backup.id())))
                .toList();
    }

    private Set<UUID> loadBookmarkedBackupIds(UUID userId) {
        return bookmarkRepositoryPort.findByOwnerUserId(userId).stream()
                .map(PersonalWorkspaceBackupBookmark::backupId)
                .collect(Collectors.toSet());
    }
}
