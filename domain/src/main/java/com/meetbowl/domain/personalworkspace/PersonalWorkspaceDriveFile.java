package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 개인 드라이브 파일 메타데이터 애그리거트다.
 *
 * <p>파일 원본은 Object Storage에 저장하고 여기서는 파일명·크기·Content-Type과 저장 경로(storageKey)만 다룬다. 삭제는 행을 지우지 않고 삭제
 * 시각을 남기는 soft delete로 처리한다.
 */
public class PersonalWorkspaceDriveFile {

    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;
    private static final int MAX_STORAGE_KEY_LENGTH = 500;

    private final UUID id;
    private final UUID ownerUserId;
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;
    private final String storageKey;
    private final Instant uploadedAt;
    private final Instant deletedAt;

    private PersonalWorkspaceDriveFile(
            UUID id,
            UUID ownerUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            Instant uploadedAt,
            Instant deletedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.uploadedAt = uploadedAt;
        this.deletedAt = deletedAt;
    }

    public static PersonalWorkspaceDriveFile create(
            UUID ownerUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            Instant uploadedAt) {
        return of(
                null,
                ownerUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                uploadedAt,
                null);
    }

    public static PersonalWorkspaceDriveFile of(
            UUID id,
            UUID ownerUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            Instant uploadedAt,
            Instant deletedAt) {
        PersonalWorkspaceCalendarEvent.requireId(ownerUserId, "파일 소유자 ID는 필수입니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                originalFileName, MAX_FILE_NAME_LENGTH, "파일명은 필수입니다.", "파일명은 255자 이하여야 합니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                storageKey, MAX_STORAGE_KEY_LENGTH, "파일 저장 경로는 필수입니다.", "파일 저장 경로는 500자 이하여야 합니다.");
        PersonalWorkspaceCalendarEvent.validateOptionalLength(
                contentType, MAX_CONTENT_TYPE_LENGTH, "파일 Content-Type은 100자 이하여야 합니다.");
        if (sizeBytes <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "파일 크기는 0보다 커야 합니다.");
        }
        if (uploadedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "파일 업로드 시각은 필수입니다.");
        }

        return new PersonalWorkspaceDriveFile(
                id,
                ownerUserId,
                originalFileName.trim(),
                PersonalWorkspaceCalendarEvent.normalize(contentType),
                sizeBytes,
                storageKey.trim(),
                uploadedAt,
                deletedAt);
    }

    public PersonalWorkspaceDriveFile delete(Instant deletedAt) {
        if (deletedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "파일 삭제 시각은 필수입니다.");
        }
        return of(
                id,
                ownerUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                uploadedAt,
                deletedAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId.equals(userId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
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

    public Instant uploadedAt() {
        return uploadedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }
}
