package com.meetbowl.application.personalworkspace.backup;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

/** 검색 범위를 사용자 본인 백업으로만 한정한다. */
@Service
public class SearchBackupsUseCase {

    private final PersonalWorkspaceBackupRepositoryPort backupRepositoryPort;
    private final PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort;

    public SearchBackupsUseCase(
            PersonalWorkspaceBackupRepositoryPort backupRepositoryPort,
            PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort) {
        this.backupRepositoryPort = backupRepositoryPort;
        this.bookmarkRepositoryPort = bookmarkRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<BackupResult> execute(UUID userId, String keyword) {
        // 빈 키워드는 전체 조회와 의미가 겹쳐 검색 의도를 알 수 없으므로 잘못된 요청으로 처리한다.
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "검색어는 필수입니다.");
        }

        Set<UUID> bookmarkedBackupIds =
                bookmarkRepositoryPort.findByOwnerUserId(userId).stream()
                        .map(PersonalWorkspaceBackupBookmark::backupId)
                        .collect(Collectors.toSet());

        return backupRepositoryPort.searchByOwnerUserIdAndKeyword(userId, keyword.trim()).stream()
                .map(backup -> BackupResult.from(backup, bookmarkedBackupIds.contains(backup.id())))
                .toList();
    }
}
