package com.meetbowl.domain.sharedworkspace;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 공유 워크스페이스 애그리거트들이 공통으로 쓰는 입력 검증 헬퍼다.
 *
 * <p>필수/선택 텍스트 길이와 식별자 존재 검증을 한곳에 모아, 워크스페이스·파일·버전이 같은 규칙을 제각각 구현해 어긋나는 것을 막는다. 선택값은 빈 문자열을 null로
 * 정규화해 "값 없음"을 일관되게 다룬다.
 */
final class SharedWorkspaceValidators {

    private SharedWorkspaceValidators() {}

    static void requireId(UUID id, String message) {
        if (id == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }

    static void requireText(
            String value, int maxLength, String requiredMessage, String maxLengthMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, requiredMessage);
        }
        if (value.trim().length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, maxLengthMessage);
        }
    }

    static void validateOptionalLength(String value, int maxLength, String message) {
        String normalized = normalize(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
