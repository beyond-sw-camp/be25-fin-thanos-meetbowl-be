package com.meetbowl.application.personalworkspace.backup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupBookmarkRepositoryPort;

/**
 * 백업 자료 북마크를 해제한다.
 *
 * <p>해제는 멱등 연산이다. 북마크가 없어도 사용자 입장의 최종 상태(북마크 없음)는 동일하므로 별도 예외 없이 성공 처리한다.
 */
@Service
public class RemoveBackupBookmarkUseCase {

    private final PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort;

    public RemoveBackupBookmarkUseCase(
            PersonalWorkspaceBackupBookmarkRepositoryPort bookmarkRepositoryPort) {
        this.bookmarkRepositoryPort = bookmarkRepositoryPort;
    }

    @Transactional
    public void execute(UUID userId, UUID backupId) {
        bookmarkRepositoryPort.deleteByOwnerUserIdAndBackupId(userId, backupId);
    }
}
