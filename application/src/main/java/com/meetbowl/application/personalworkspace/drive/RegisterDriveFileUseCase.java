package com.meetbowl.application.personalworkspace.drive;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.document.DocumentIndexRequestedEvent;
import com.meetbowl.domain.document.DocumentIndexRequestedEventPort;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFile;
import com.meetbowl.domain.personalworkspace.PersonalWorkspaceDriveFileRepositoryPort;
import com.meetbowl.domain.storage.ObjectStoragePort;

/**
 * 개인 드라이브 파일 원본을 Object Storage에 저장하고 메타데이터를 등록한다.
 *
 * <p>실제 바이너리는 Object Storage에 저장하고 DB에는 메타데이터만 남긴다. 저장 경로는 사용자별로 충돌하지 않도록 {@code
 * personal-drive/{userId}/{uuid}} 형식으로 서버가 생성한다. 저장 완료 후 AI가 S3에서 파일을 가져가 텍스트를 추출하도록 색인 이벤트를 발행한다.
 */
@Service
public class RegisterDriveFileUseCase {

    private static final String STORAGE_KEY_PREFIX = "personal-drive/";
    private static final String PERSONAL_DRIVE_FILE_DOCUMENT_TYPE = "PERSONAL_DRIVE_FILE";
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES =
            Map.of(
                    "pdf", "application/pdf",
                    "png", "image/png",
                    "jpg", "image/jpeg",
                    "jpeg", "image/jpeg",
                    "docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "txt", "text/plain");

    private final PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentIndexRequestedEventPort documentIndexRequestedEventPort;

    public RegisterDriveFileUseCase(
            PersonalWorkspaceDriveFileRepositoryPort driveFileRepositoryPort,
            ObjectStoragePort objectStoragePort,
            DocumentIndexRequestedEventPort documentIndexRequestedEventPort) {
        this.driveFileRepositoryPort = driveFileRepositoryPort;
        this.objectStoragePort = objectStoragePort;
        this.documentIndexRequestedEventPort = documentIndexRequestedEventPort;
    }

    @Transactional
    public DriveFileResult execute(RegisterDriveFileCommand command) {
        byte[] content = requireContent(command.content());
        String contentType = resolveContentType(command.originalFileName());
        String storageKey = generateStorageKey(command.ownerUserId());

        // DB에는 원본을 넣지 않는다. S3 업로드가 성공한 경우에만 업무 메타데이터를 생성한다.
        objectStoragePort.upload(storageKey, contentType, content);

        PersonalWorkspaceDriveFile file =
                PersonalWorkspaceDriveFile.create(
                        command.ownerUserId(),
                        command.originalFileName(),
                        contentType,
                        content.length,
                        storageKey,
                        Instant.now());

        PersonalWorkspaceDriveFile saved = driveFileRepositoryPort.save(file);

        // 파일 본문은 RabbitMQ에 싣지 않는다. AI가 storageKey로 S3에서 내려받아 추출·임베딩한다.
        documentIndexRequestedEventPort.publish(
                new DocumentIndexRequestedEvent(
                        saved.id(),
                        PERSONAL_DRIVE_FILE_DOCUMENT_TYPE,
                        command.organizationId(),
                        saved.ownerUserId(),
                        saved.originalFileName(),
                        null,
                        saved.storageKey(),
                        saved.contentType(),
                        List.of(saved.ownerUserId()),
                        List.of(),
                        List.of()));

        return DriveFileResult.from(saved);
    }

    private byte[] requireContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "업로드할 파일은 필수입니다.");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "파일은 20MB 이하여야 합니다.");
        }
        return content;
    }

    private String resolveContentType(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "파일명은 필수입니다.");
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        String extension =
                dotIndex < 0
                        ? ""
                        : originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new BusinessException(
                    ErrorCode.FILE_INVALID_EXTENSION,
                    "PDF, PNG, JPG, JPEG, DOCX, TXT 파일만 업로드할 수 있습니다.");
        }
        return contentType;
    }

    private String generateStorageKey(UUID ownerUserId) {
        return STORAGE_KEY_PREFIX + ownerUserId + "/" + UUID.randomUUID();
    }
}
