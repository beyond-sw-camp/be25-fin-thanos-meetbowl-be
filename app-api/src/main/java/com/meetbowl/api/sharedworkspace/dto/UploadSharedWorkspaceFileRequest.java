package com.meetbowl.api.sharedworkspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 공유 자료 업로드 요청 DTO다. 파일 원본은 Object Storage에 먼저 올린 뒤 그 저장 경로(storageKey)와 메타데이터만 전달한다. 원본을 본문으로 받지
 * 않는 이유는 DB에 파일을 저장하지 않는다는 규칙 때문이다.
 */
public record UploadSharedWorkspaceFileRequest(
        @NotBlank(message = "파일명은 필수입니다.") String originalFileName,
        String contentType,
        @Positive(message = "파일 크기는 0보다 커야 합니다.") long sizeBytes,
        @NotBlank(message = "파일 저장 경로는 필수입니다.") String storageKey) {}
