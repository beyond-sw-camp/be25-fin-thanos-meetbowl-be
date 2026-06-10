package com.meetbowl.domain.mail;

import java.util.Objects;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class MailAttachment {

    private final UUID id;
    private final UUID uploaderUserId;
    private final String objectKey;
    private final String originalFileName;
    private final String storedFileName;
    private final String mimeType;
    private final long sizeBytes;

    private MailAttachment(
            UUID id,
            UUID uploaderUserId,
            String objectKey,
            String originalFileName,
            String storedFileName,
            String mimeType,
            long sizeBytes) {
        this.id = id;
        this.uploaderUserId = requireNonNull(uploaderUserId, "첨부파일 업로더 ID는 필수입니다.");
        this.objectKey = requireText(objectKey, "첨부파일 object key는 필수입니다.");
        this.originalFileName = requireText(originalFileName, "첨부파일 원본 파일명은 필수입니다.");
        this.storedFileName = requireText(storedFileName, "첨부파일 저장 파일명은 필수입니다.");
        this.mimeType = requireText(mimeType, "첨부파일 MIME type은 필수입니다.");
        if (sizeBytes <= 0) {
            throw invalid("첨부파일 크기는 0보다 커야 합니다.");
        }
        this.sizeBytes = sizeBytes;
    }

    public static MailAttachment create(
            UUID uploaderUserId,
            String objectKey,
            String originalFileName,
            String storedFileName,
            String mimeType,
            long sizeBytes) {
        return of(
                null,
                uploaderUserId,
                objectKey,
                originalFileName,
                storedFileName,
                mimeType,
                sizeBytes);
    }

    public static MailAttachment of(
            UUID id,
            UUID uploaderUserId,
            String objectKey,
            String originalFileName,
            String storedFileName,
            String mimeType,
            long sizeBytes) {
        return new MailAttachment(
                id,
                uploaderUserId,
                objectKey,
                originalFileName,
                storedFileName,
                mimeType,
                sizeBytes);
    }

    public UUID id() {
        return id;
    }

    public UUID uploaderUserId() {
        return uploaderUserId;
    }

    public String objectKey() {
        return objectKey;
    }

    public String originalFileName() {
        return originalFileName;
    }

    public String storedFileName() {
        return storedFileName;
    }

    public String mimeType() {
        return mimeType;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw invalid(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
        return value;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }
}
