package com.meetbowl.application.sharedworkspace;

import java.util.Locale;
import java.util.Map;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 공유 자료 업로드(신규/새 버전)에서 공통으로 쓰는 파일 검증 로직이다.
 *
 * <p>신뢰할 Content-Type은 클라이언트 값이 아니라 서버가 확장자로 판정하고, 최대 크기(20MB)와 허용 형식을 두 흐름에서 동일하게 강제한다.
 */
final class SharedWorkspaceFileUploadSupport {

    static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES =
            Map.of(
                    "pdf", "application/pdf",
                    "png", "image/png",
                    "jpg", "image/jpeg",
                    "jpeg", "image/jpeg",
                    "docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "txt", "text/plain");

    private SharedWorkspaceFileUploadSupport() {}

    static byte[] requireContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "업로드할 파일은 필수입니다.");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "파일은 20MB 이하여야 합니다.");
        }
        return content;
    }

    static String resolveContentType(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "파일명은 필수입니다.");
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        String extension =
                dotIndex < 0
                        ? ""
                        : originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new BusinessException(
                    ErrorCode.FILE_INVALID_EXTENSION,
                    "PDF, PNG, JPG, JPEG, DOCX, TXT 파일만 업로드할 수 있습니다.");
        }
        return contentType;
    }
}
