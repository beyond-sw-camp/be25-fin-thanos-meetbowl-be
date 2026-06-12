package com.meetbowl.api.personalworkspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 개인 드라이브 파일 메타데이터 등록 요청 DTO다.
 *
 * <p>실제 바이너리 업로드 전 단계의 메타데이터 등록 요청이다. storageKey는 서버가 생성하므로 받지 않는다.
 */
public record RegisterDriveFileRequest(
        @NotBlank(message = "파일명은 필수입니다.") String originalFileName,
        String contentType,
        @Positive(message = "파일 크기는 0보다 커야 합니다.") long sizeBytes) {}
