package com.meetbowl.domain.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class SharedWorkspaceFile {

    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;
    private static final int MAX_STORAGE_KEY_LENGTH = 500;

    private final UUID id;
    private final UUID workspaceId;
    private final UUID uploaderUserId;
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;
    private final String storageKey;
    private final DocumentVersion currentVersion;
    private final Instant uploadedAt;
    private final Instant deletedAt;

    private SharedWorkspaceFile(
            UUID id,
            UUID workspaceId,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            DocumentVersion currentVersion,
            Instant uploadedAt,
            Instant deletedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.uploaderUserId = uploaderUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.currentVersion = currentVersion;
        this.uploadedAt = uploadedAt;
        this.deletedAt = deletedAt;
    }

    public static SharedWorkspaceFile create(
            UUID workspaceId,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            Instant uploadedAt) {
        return of(
                null,
                workspaceId,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                DocumentVersion.INITIAL,
                uploadedAt,
                null);
    }

    public static SharedWorkspaceFile of(
            UUID id,
            UUID workspaceId,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            DocumentVersion currentVersion,
            Instant uploadedAt,
            Instant deletedAt) {
        SharedWorkspaceValidators.requireId(workspaceId, "공유 워크스페이스 ID는 필수입니다.");
        SharedWorkspaceValidators.requireId(uploaderUserId, "공유 파일 업로더 ID는 필수입니다.");
        SharedWorkspaceValidators.requireText(
                originalFileName, MAX_FILE_NAME_LENGTH, "공유 파일명은 필수입니다.", "공유 파일명은 255자 이하여야 합니다.");
        SharedWorkspaceValidators.requireText(
                storageKey,
                MAX_STORAGE_KEY_LENGTH,
                "공유 파일 저장 경로는 필수입니다.",
                "공유 파일 저장 경로는 500자 이하여야 합니다.");
        SharedWorkspaceValidators.validateOptionalLength(
                contentType, MAX_CONTENT_TYPE_LENGTH, "공유 파일 Content-Type은 100자 이하여야 합니다.");
        if (sizeBytes <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 크기는 0보다 커야 합니다.");
        }
        if (currentVersion == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 현재 버전은 필수입니다.");
        }
        if (uploadedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 업로드 시각은 필수입니다.");
        }

        return new SharedWorkspaceFile(
                id,
                workspaceId,
                uploaderUserId,
                originalFileName.trim(),
                SharedWorkspaceValidators.normalize(contentType),
                sizeBytes,
                storageKey.trim(),
                currentVersion,
                uploadedAt,
                deletedAt);
    }

    public SharedWorkspaceFile addVersion(
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            DocumentVersion expectedCurrentVersion,
            DocumentVersion newVersion,
            Instant uploadedAt) {
        ensureActive();
        if (!currentVersion.equals(expectedCurrentVersion)) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT, "다른 사용자가 먼저 저장했습니다. 최신 버전을 확인한 후 다시 저장해 주세요.");
        }
        if (newVersion == null || newVersion.compareTo(currentVersion) <= 0) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "새 문서 버전은 현재 버전보다 커야 합니다.");
        }
        return of(
                id,
                workspaceId,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                newVersion,
                uploadedAt,
                deletedAt);
    }

    public SharedWorkspaceFile delete(Instant deletedAt) {
        ensureActive();
        if (deletedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 삭제 시각은 필수입니다.");
        }
        return of(
                id,
                workspaceId,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                currentVersion,
                uploadedAt,
                deletedAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID id() {
        return id;
    }

    public UUID workspaceId() {
        return workspaceId;
    }

    public UUID uploaderUserId() {
        return uploaderUserId;
    }

    public String originalFileName() {
        return originalFileName;
    }

    public String contentType() {
        return contentType;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public String storageKey() {
        return storageKey;
    }

    public DocumentVersion currentVersion() {
        return currentVersion;
    }

    public Instant uploadedAt() {
        return uploadedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    private void ensureActive() {
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "삭제된 공유 파일은 변경할 수 없습니다.");
        }
    }
}
