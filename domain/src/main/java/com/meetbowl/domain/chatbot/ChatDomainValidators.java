package com.meetbowl.domain.chatbot;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/** 챗봇 계약의 검증 문구와 정규화 기준이 모델마다 달라지는 것을 막기 위한 패키지 내부 정책이다. */
final class ChatDomainValidators {

    private ChatDomainValidators() {}

    static void requireId(UUID id, String message) {
        if (id == null) {
            throw invalid(message);
        }
    }

    static String requireText(
            String value, int maxLength, String requiredMessage, String maxLengthMessage) {
        if (value == null || value.isBlank()) {
            throw invalid(requiredMessage);
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw invalid(maxLengthMessage);
        }
        return normalized;
    }

    static String normalizeOptional(String value, int maxLength, String maxLengthMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw invalid(maxLengthMessage);
        }
        return normalized;
    }

    static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, message);
    }
}
