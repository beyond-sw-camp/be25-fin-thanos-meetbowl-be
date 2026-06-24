package com.meetbowl.application.personalworkspace.backup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackup;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceBackupRepositoryPort;

/** 현재 사용자가 소유한 개인 워크스페이스 백업의 본문을 조회한다. */
@Service
public class GetBackupDetailUseCase {

    private final PersonalWorkspaceBackupRepositoryPort backupRepositoryPort;

    public GetBackupDetailUseCase(PersonalWorkspaceBackupRepositoryPort backupRepositoryPort) {
        this.backupRepositoryPort = backupRepositoryPort;
    }

    @Transactional(readOnly = true)
    public BackupDetailResult execute(UUID userId, UUID backupId) {
        PersonalWorkspaceBackup backup =
                backupRepositoryPort
                        .findById(backupId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "백업 자료를 찾을 수 없습니다."));
        if (!backup.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "본인 백업 자료만 조회할 수 있습니다.");
        }
        return BackupDetailResult.from(backup);
    }
}
