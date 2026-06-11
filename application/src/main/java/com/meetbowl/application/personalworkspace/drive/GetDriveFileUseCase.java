package com.meetbowl.application.personalworkspace.drive;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/**
 * 개인 드라이브 파일의 다운로드 정보를 조회한다.
 *
 * <p>파일 식별자는 전역 UUID이므로 소유자 검증을 반드시 수행한다. soft delete된 파일은 다운로드 대상이 아니므로 존재하지 않는 것으로 처리한다.
 */
@Service
public class GetDriveFileUseCase {

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;

    public GetDriveFileUseCase(PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
    }

    @Transactional(readOnly = true)
    public DriveFileResult execute(UUID userId, UUID fileId) {
        PersonalWorkspaceDriveFile file =
                driveFileRepositoryPort
                        .findById(fileId)
                        .filter(found -> !found.isDeleted())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "파일을 찾을 수 없습니다."));

        if (!file.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "본인 파일만 접근할 수 있습니다.");
        }

        return DriveFileResult.from(file);
    }
}
