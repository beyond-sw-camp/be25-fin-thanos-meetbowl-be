package com.meetbowl.domain.chatbot;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 챗봇 도메인 객체가 공통으로 사용하는 입력 검증 도우미다.
 *
 * <p>도메인 객체는 생성 시점부터 유효한 상태를 유지해야 하므로, 세션·메시지·출처 객체의 팩토리 메서드가 이 클래스를 통해 필수값과 문자열 길이를 검증한다. 이 클래스는
 * 챗봇 패키지 내부 구현 세부사항이므로 외부 계층에 공개하지 않는다.
 */
final class ChatDomainValidators {

    private ChatDomainValidators() {}

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

    static void requireInstant(Instant instant, String message) {
        if (instant == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
        }
    }

    /** 선택 문자열의 앞뒤 공백을 제거하고, null 또는 공백뿐인 값은 저장하지 않도록 null로 정규화한다. */
    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
