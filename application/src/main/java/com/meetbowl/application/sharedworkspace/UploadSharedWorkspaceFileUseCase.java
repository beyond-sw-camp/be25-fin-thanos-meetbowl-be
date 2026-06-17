package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFile;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersion;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceFileVersionRepositoryPort;
import com.meetbowl.domain.storage.ObjectStoragePort;

/**
 * 공유 자료를 업로드한다. 멤버만 가능하다. 최초 등록 시 파일 메타데이터와 v.1.0.0 버전 이력을 같은 트랜잭션에서 함께 만들어, 이후 모든 버전이 동일한 이력 테이블에서
 * 일관되게 추적되도록 한다. 파일 원본은 S3에 저장하고, 저장 후 AI가 파일을 내려받아 색인하도록 이벤트를 발행한다.
 */
@Service
public class UploadSharedWorkspaceFileUseCase {

    private static final String STORAGE_KEY_PREFIX = "shared-workspace/";
    private static final String SHARED_WORKSPACE_FILE_DOCUMENT_TYPE =
            "SHARED_WORKSPACE_FILE_VERSION";

    private final SharedWorkspaceFileRepositoryPort fileRepositoryPort;
    private final SharedWorkspaceFileVersionRepositoryPort versionRepositoryPort;
    private final SharedWorkspaceAccessGuard accessGuard;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;
    private final Clock clock;

    public UploadSharedWorkspaceFileUseCase(
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
    public SharedWorkspaceFileResult execute(UploadSharedWorkspaceFileCommand command) {
        accessGuard.requireActiveMember(command.workspaceId(), command.uploaderUserId());

        byte[] content = SharedWorkspaceFileUploadSupport.requireContent(command.content());
        String contentType =
                SharedWorkspaceFileUploadSupport.resolveContentType(command.originalFileName());
        String storageKey = generateStorageKey(command.workspaceId());

        // DB에는 원본을 넣지 않는다. S3 업로드가 성공한 경우에만 업무 메타데이터를 생성한다.
        objectStoragePort.upload(storageKey, contentType, content);

        Instant now = Instant.now(clock);
        SharedWorkspaceFile file =
                fileRepositoryPort.save(
                        SharedWorkspaceFile.create(
                                command.workspaceId(),
                                command.uploaderUserId(),
                                command.originalFileName(),
                                contentType,
                                content.length,
                                storageKey,
                                now));

        versionRepositoryPort.save(
                SharedWorkspaceFileVersion.create(
                        file.id(),
                        file.currentVersion(),
                        command.uploaderUserId(),
                        command.originalFileName(),
                        contentType,
                        content.length,
                        storageKey,
                        null,
                        now));

        publishIndexEvent(command, file.id(), storageKey, contentType);
        return SharedWorkspaceFileResult.from(file);
    }

    private void publishIndexEvent(
            UploadSharedWorkspaceFileCommand command,
            UUID fileId,
            String storageKey,
            String contentType) {
        // 색인 단위는 파일(documentId=fileId)이라 새 버전이 올라오면 같은 문서를 최신 내용으로 교체한다.
        // 공유 자료이므로 접근 범위는 워크스페이스 단위(sharedWorkspaceIds)로 멤버 전원이 검색할 수 있게 한다.
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        fileId,
                        SHARED_WORKSPACE_FILE_DOCUMENT_TYPE,
                        command.organizationId(),
                        command.uploaderUserId(),
                        command.originalFileName(),
                        // 파일 문서라 content 대신 S3 위치/타입을 metadata에 담고, 어느 워크스페이스 자료인지도 함께 식별한다.
                        null,
                        new DocumentIndexRequestedEvent.Metadata(
                                null,
                                null,
                                command.workspaceId(),
                                null,
                                null,
                                storageKey,
                                contentType),
                        List.of(),
                        List.of(),
                        List.of(command.workspaceId())));
    }

    private String generateStorageKey(UUID workspaceId) {
        return STORAGE_KEY_PREFIX + workspaceId + "/" + UUID.randomUUID();
    }
}
