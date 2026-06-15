package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.sharedworkspace.DocumentVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;
import com.meetbowl.domain.storage.ObjectStoragePort;

/**
 * 기존 공유 자료에 새 버전을 추가한다. 동시에 두 명이 같은 파일을 수정하면 한쪽 변경이 사라질 수 있어, 파일 행을 잠금 조회한 뒤 요청의 기대 버전과 DB 현재 버전을
 * 다시 비교한다. 기대 버전이 어긋나면 도메인이 충돌로 막고, 호출자는 최신 버전을 확인한 후 재시도하도록 409로 안내받는다. 새 버전 원본은 S3에 저장하고, 색인 단위는
 * 파일이므로 같은 documentId로 최신 내용을 재색인하도록 이벤트를 발행한다.
 */
@Service
public class AddSharedWorkspaceFileVersionUseCase {

    private static final String STORAGE_KEY_PREFIX = "shared-workspace/";
    private static final String SHARED_WORKSPACE_FILE_DOCUMENT_TYPE = "SHARED_WORKSPACE_FILE_VERSION";

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;
    private final Clock clock;

    public AddSharedWorkspaceFileVersionUseCase(
            SharedWorkspaceFileRepositoryPort fileRepositoryPort,
            SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort,
            SharedWorkspaceAccessGuard accessGuard,
            ObjectStoragePort objectStoragePort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort,
            Clock clock) {
        this.fileRepositoryPort = fileRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
        this.accessGuard = accessGuard;
        this.objectStoragePort = objectStoragePort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
        this.clock = clock;
    }

    @Transactional
    public SharedWorkspaceFileVersionResult execute(AddSharedWorkspaceFileVersionCommand command) {
        accessGuard.requireActiveMember(command.workspaceId(), command.uploaderUserId());

        DocumentVersion expected = DocumentVersion.parse(command.expectedCurrentVersion());
        DocumentVersion newVersion = DocumentVersion.parse(command.newVersion());

        byte[] content = SharedWorkspaceFileUploadSupport.requireContent(command.content());
        String contentType =
                SharedWorkspaceFileUploadSupport.resolveContentType(command.originalFileName());
        // 버전마다 별도 storageKey를 둬서 이전 버전 원본도 S3에 보존한다.
        String storageKey = generateStorageKey(command.workspaceId());

        // 잠금 조회로 현재 버전을 고정한 뒤에야 충돌 검사와 버전 증가가 직렬화된다.
        SharedWorkspaceFile file =
                fileRepositoryPort
                        .findByIdForUpdate(command.fileId())
                        .filter(found -> !found.isDeleted())
                        .filter(found -> found.workspaceId().equals(command.workspaceId()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.SHARED_WORKSPACE_FILE_NOT_FOUND));

        objectStoragePort.upload(storageKey, contentType, content);

        Instant now = Instant.now(clock);
        SharedWorkspaceFile updated =
                file.addVersion(
                        command.uploaderUserId(),
                        command.originalFileName(),
                        contentType,
                        content.length,
                        storageKey,
                        expected,
                        newVersion,
                        now);
        fileRepositoryPort.save(updated);

        SharedWorkspaceFileVersion version =
                versionRepositoryPort.save(
                        SharedWorkspaceFileVersion.create(
                                updated.id(),
                                newVersion,
                                command.uploaderUserId(),
                                command.originalFileName(),
                                contentType,
                                content.length,
                                storageKey,
                                command.changeMemo(),
                                now));

        publishIndexEvent(command, updated.id(), storageKey, contentType);
        return SharedWorkspaceFileVersionResult.from(version);
    }

    private void publishIndexEvent(
            AddSharedWorkspaceFileVersionCommand command,
            UUID fileId,
            String storageKey,
            String contentType) {
        // 색인 단위는 파일(documentId=fileId)이므로 새 버전 발행은 같은 문서를 최신 내용으로 교체한다.
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        fileId,
                        SHARED_WORKSPACE_FILE_DOCUMENT_TYPE,
                        command.organizationId(),
                        command.uploaderUserId(),
                        command.originalFileName(),
                        null,
                        storageKey,
                        contentType,
                        List.of(),
                        List.of(),
                        List.of(command.workspaceId())));
    }

    private String generateStorageKey(UUID workspaceId) {
        return STORAGE_KEY_PREFIX + workspaceId + "/" + UUID.randomUUID();
    }
}
