package com.meetbowl.domain.meeting;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의 참고자료(첨부파일) 메타데이터 도메인 모델이다.
 *
 * <p>"첨부파일이 여러 개"라 회의 컬럼이 아닌 별도 행으로 관리한다. 원본 파일은 Object Storage에 저장하고 DB에는 메타데이터만 둔다({@code
 * objectKey}로 원본 위치 참조). 회의는 {@code meetingId}, 업로더는 {@code uploadedByUserId} raw UUID로 참조한다.
 */
public class MeetingAttachment {

    private final UUID id;

    /** 소속 회의(FK). */
    private final UUID meetingId;

    /** 업로드한 사용자(FK). */
    private final UUID uploadedByUserId;

    /** Object Storage 객체 키(원본 위치). DB에 원본을 저장하지 않는다. */
    private final String objectKey;

    /** 사용자가 올린 원본 파일명. */
    private final String originalFileName;

    /** MIME 타입(예: application/pdf). */
    private final String mimeType;

    /** 파일 크기(바이트). */
    private final long sizeBytes;

    private MeetingAttachment(
            UUID id,
            UUID meetingId,
            UUID uploadedByUserId,
            String objectKey,
            String originalFileName,
            String mimeType,
            long sizeBytes) {
        this.id = id;
        this.meetingId = meetingId;
        this.uploadedByUserId = uploadedByUserId;
        this.objectKey = objectKey;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    public static MeetingAttachment create(
            UUID meetingId,
            UUID uploadedByUserId,
            String objectKey,
            String originalFileName,
            String mimeType,
            long sizeBytes) {
        return of(
                null,
                meetingId,
                uploadedByUserId,
                objectKey,
                originalFileName,
                mimeType,
                sizeBytes);
    }

    public static MeetingAttachment of(
            UUID id,
            UUID meetingId,
            UUID uploadedByUserId,
            String objectKey,
            String originalFileName,
            String mimeType,
            long sizeBytes) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의는 필수입니다.");
        }
        if (uploadedByUserId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "업로더는 필수입니다.");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "첨부파일 저장 키는 필수입니다.");
        }
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "원본 파일명은 필수입니다.");
        }
        if (sizeBytes < 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "파일 크기는 0 이상이어야 합니다.");
        }
        return new MeetingAttachment(
                id, meetingId, uploadedByUserId, objectKey, originalFileName, mimeType, sizeBytes);
    }

    public UUID id() {
        return id;
    }

    public UUID meetingId() {
        return meetingId;
    }

    public UUID uploadedByUserId() {
        return uploadedByUserId;
    }

    public String objectKey() {
        return objectKey;
    }

    public String originalFileName() {
        return originalFileName;
    }

    public String mimeType() {
        return mimeType;
    }

    public long sizeBytes() {
        return sizeBytes;
    }
}
