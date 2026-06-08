package com.meetbowl.domain.sharedworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class SharedWorkspaceFileVersion {

    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;
    private static final int MAX_STORAGE_KEY_LENGTH = 500;
    private static final int MAX_CHANGE_MEMO_LENGTH = 1000;

    private final UUID id;
    private final UUID fileId;
    private final int versionNumber;
    private final UUID uploaderUserId;
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;
    private final String storageKey;
    private final String changeMemo;
    private final Instant uploadedAt;

    private SharedWorkspaceFileVersion(
            UUID id,
            UUID fileId,
            int versionNumber,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String changeMemo,
            Instant uploadedAt) {
        this.id = id;
        this.fileId = fileId;
        this.versionNumber = versionNumber;
        this.uploaderUserId = uploaderUserId;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.changeMemo = changeMemo;
        this.uploadedAt = uploadedAt;
    }

    public static SharedWorkspaceFileVersion create(
            UUID fileId,
            int versionNumber,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String changeMemo,
            Instant uploadedAt) {
        return of(
                null,
                fileId,
                versionNumber,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                changeMemo,
                uploadedAt);
    }

    public static SharedWorkspaceFileVersion of(
            UUID id,
            UUID fileId,
            int versionNumber,
            UUID uploaderUserId,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String storageKey,
            String changeMemo,
            Instant uploadedAt) {
        SharedWorkspaceValidators.requireId(fileId, "공유 파일 ID는 필수입니다.");
        SharedWorkspaceValidators.requireId(uploaderUserId, "공유 파일 버전 업로더 ID는 필수입니다.");
        SharedWorkspaceValidators.requireText(
                originalFileName,
                MAX_FILE_NAME_LENGTH,
                "공유 파일 버전 파일명은 필수입니다.",
                "공유 파일 버전 파일명은 255자 이하여야 합니다.");
        SharedWorkspaceValidators.requireText(
                storageKey,
                MAX_STORAGE_KEY_LENGTH,
                "공유 파일 버전 저장 경로는 필수입니다.",
                "공유 파일 버전 저장 경로는 500자 이하여야 합니다.");
        SharedWorkspaceValidators.validateOptionalLength(
                contentType, MAX_CONTENT_TYPE_LENGTH, "공유 파일 버전 Content-Type은 100자 이하여야 합니다.");
        SharedWorkspaceValidators.validateOptionalLength(
                changeMemo, MAX_CHANGE_MEMO_LENGTH, "공유 파일 버전 변경 메모는 1000자 이하여야 합니다.");
        if (versionNumber < 1) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 버전 번호는 1 이상이어야 합니다.");
        }
        if (sizeBytes <= 0) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 버전 크기는 0보다 커야 합니다.");
        }
        if (uploadedAt == null) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "공유 파일 버전 업로드 시각은 필수입니다.");
        }

        return new SharedWorkspaceFileVersion(
                id,
                fileId,
                versionNumber,
                uploaderUserId,
                originalFileName.trim(),
                SharedWorkspaceValidators.normalize(contentType),
                sizeBytes,
                storageKey.trim(),
                SharedWorkspaceValidators.normalize(changeMemo),
                uploadedAt);
    }

    public SharedWorkspaceFileVersion updateChangeMemo(String changeMemo) {
        return of(
                id,
                fileId,
                versionNumber,
                uploaderUserId,
                originalFileName,
                contentType,
                sizeBytes,
                storageKey,
                changeMemo,
                uploadedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID fileId() {
        return fileId;
    }

    public int versionNumber() {
        return versionNumber;
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

    public String changeMemo() {
        return changeMemo;
    }

    public Instant uploadedAt() {
        return uploadedAt;
    }
}
