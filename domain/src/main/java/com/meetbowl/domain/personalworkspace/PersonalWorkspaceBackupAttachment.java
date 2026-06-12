package com.meetbowl.domain.personalworkspace;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 개인 워크스페이스 백업에 포함된 첨부파일 스냅샷이다.
 *
 * <p>원본 메일이 보존 정책으로 삭제돼도 백업이 자기완결적으로 남도록, 백업 시점의 첨부 메타데이터(S3 object key 포함)를 복사해 보관한다. 파일 실체(S3 객체)
 * 복사는 후속 단계에서 object key를 기준으로 수행한다.
 */
public class PersonalWorkspaceBackupAttachment {

    private final String objectKey;
    private final String originalFileName;
    private final String storedFileName;
    private final String mimeType;
    private final long sizeBytes;

    private PersonalWorkspaceBackupAttachment(
            String objectKey,
            String originalFileName,
            String storedFileName,
            String mimeType,
            long sizeBytes) {
        this.objectKey = requireText(objectKey, "백업 첨부 object key는 필수입니다.");
        this.originalFileName = requireText(originalFileName, "백업 첨부 원본 파일명은 필수입니다.");
        this.storedFileName = requireText(storedFileName, "백업 첨부 저장 파일명은 필수입니다.");
        this.mimeType = requireText(mimeType, "백업 첨부 MIME type은 필수입니다.");
        if (sizeBytes <= 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "백업 첨부 크기는 0보다 커야 합니다.");
        }
        this.sizeBytes = sizeBytes;
    }

    public static PersonalWorkspaceBackupAttachment of(
            String objectKey,
            String originalFileName,
            String storedFileName,
            String mimeType,
            long sizeBytes) {
        return new PersonalWorkspaceBackupAttachment(
                objectKey, originalFileName, storedFileName, mimeType, sizeBytes);
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

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
        return value;
    }
}
