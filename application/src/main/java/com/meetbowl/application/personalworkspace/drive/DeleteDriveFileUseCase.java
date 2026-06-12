package com.meetbowl.application.personalworkspace.drive;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/**
 * 개인 드라이브 파일을 삭제한다.
 *
 * <p>물리 삭제 대신 deletedAt 기반 soft delete로 처리한다. 이미 삭제된 파일은 최종 상태가 같으므로 멱등하게 성공 처리하고, 소유자가 아닌 경우에는 접근을
 * 차단한다.
 */
@Service
public class DeleteDriveFileUseCase {

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;

    public DeleteDriveFileUseCase(
            PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
    }

    @Transactional
    public void execute(UUID userId, UUID fileId) {
        PersonalWorkspaceDriveFile file =
                driveFileRepositoryPort
                        .findById(fileId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "삭제할 파일을 찾을 수 없습니다."));

        if (!file.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "본인 파일만 삭제할 수 있습니다.");
        }

        if (file.isDeleted()) {
            return;
        }

        driveFileRepositoryPort.save(file.delete(Instant.now()));
    }
}
