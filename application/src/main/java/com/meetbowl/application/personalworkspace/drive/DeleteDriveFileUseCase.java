package com.meetbowl.application.personalworkspace.drive;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRemovedEvent;
import com.meetbowl.domain.document.DocumentIndexRemovedEventPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;

/**
 * 개인 드라이브 파일을 삭제한다.
 *
 * <p>물리 삭제 대신 deletedAt 기반 soft delete로 처리한다. 이미 삭제된 파일은 최종 상태가 같으므로 멱등하게 성공 처리하고, 소유자가 아닌 경우에는 접근을
 * 차단한다. 원본은 복구 가능하도록 S3에 보존하되, 챗봇 검색에는 더 이상 나오지 않도록 색인 제거 이벤트를 발행한다.
 */
@Service
public class DeleteDriveFileUseCase {

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;
    private final DocumentIndexRemovedEventPort documentIndexRemovedEventPort;

    public DeleteDriveFileUseCase(
            PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort,
            DocumentIndexRemovedEventPort documentIndexRemovedEventPort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
        this.documentIndexRemovedEventPort = documentIndexRemovedEventPort;
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
        // 색인 단위는 파일(documentId=fileId)이므로 fileId로 색인 제거를 요청한다.
        documentIndexRemovedEventPort.publish(new DocumentIndexRemovedEvent(fileId));
    }
}
