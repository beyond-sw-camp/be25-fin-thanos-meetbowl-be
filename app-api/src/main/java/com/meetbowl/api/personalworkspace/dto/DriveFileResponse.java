package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.personalworkspace.drive.DriveFileResult;

/** 개인 드라이브 파일 응답 DTO다. 다운로드는 storageKey 기반으로 스토리지 어댑터 연동 후속 작업에서 처리한다. */
public record DriveFileResponse(
        UUID fileId,
        UUID ownerUserId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        Instant uploadedAt) {

    public static DriveFileResponse from(DriveFileResult result) {
        return new DriveFileResponse(
                result.fileId(),
                result.ownerUserId(),
                result.originalFileName(),
                result.contentType(),
                result.sizeBytes(),
                result.storageKey(),
                result.uploadedAt());
    }
}
