package com.meetbowl.api.sharedworkspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 새 파일 버전 업로드 요청 DTO다. expectedCurrentVersion은 클라이언트가 마지막으로 본 현재 버전으로, 동시 수정 충돌 검사에 쓴다. newVersion은
 * 수정자가 지정하는 major.minor.patch 값이며 형식 검증은 도메인 DocumentVersion이 수행한다.
 */
public record AddSharedWorkspaceFileVersionRequest(
        @NotBlank(message = "파일명은 필수입니다.") String originalFileName,
        String contentType,
        @Positive(message = "파일 크기는 0보다 커야 합니다.") long sizeBytes,
        @NotBlank(message = "파일 저장 경로는 필수입니다.") String storageKey,
        @NotBlank(message = "기대 현재 버전은 필수입니다.") String expectedCurrentVersion,
        @NotBlank(message = "새 버전은 필수입니다.") String newVersion,
        @Size(max = 1000, message = "변경 메모는 1000자 이하여야 합니다.") String changeMemo) {}
