package com.meetbowl.application.personalworkspace.backup;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmark;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

/**
 * 백업 자료에 북마크를 등록한다.
 *
 * <p>북마크 대상이 실제로 존재하고 본인 소유인지 먼저 확인한다. 이미 북마크가 있으면 새로 저장하지 않고 멱등하게 성공 처리해 중복 요청에도 동일 결과를 보장한다.
 */
@Service
public class AddBackupBookmarkUseCase {

    private final PersonalWorkspaceBackupRepositoryPort backupRepositoryPort;
    private final PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort;

    public AddBackupBookmarkUseCase(
            PersonalWorkspaceBackupRepositoryPort backupRepositoryPort,
            PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort) {
        this.backupRepositoryPort = backupRepositoryPort;
        this.bookmarkRepositoryPort = bookmarkRepositoryPort;
    }

    @Transactional
    public void execute(UUID userId, UUID backupId) {
        PersonalWorkspaceBackup backup =
                backupRepositoryPort
                        .findById(backupId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "백업 자료를 찾을 수 없습니다."));

        if (!backup.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "본인 백업 자료만 북마크할 수 있습니다.");
        }

        if (bookmarkRepositoryPort.findByOwnerUserIdAndBackupId(userId, backupId).isPresent()) {
            return;
        }

        bookmarkRepositoryPort.save(
                PersonalWorkspaceBackupBookmark.create(userId, backupId, Instant.now()));
    }
}
