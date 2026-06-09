package com.meetbowl.domain.sharedworkspace;

import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

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
