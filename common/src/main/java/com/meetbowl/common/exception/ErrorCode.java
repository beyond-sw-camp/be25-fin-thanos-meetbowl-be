package com.meetbowl.common.exception;

public enum ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", 400),
    COMMON_INVALID_REQUEST("COMMON_INVALID_REQUEST", "잘못된 요청입니다.", 400),
    COMMON_UNAUTHORIZED("COMMON_UNAUTHORIZED", "인증이 필요합니다.", 401),
    COMMON_FORBIDDEN("COMMON_FORBIDDEN", "권한이 없습니다.", 403),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", "리소스를 찾을 수 없습니다.", 404),
    COMMON_CONFLICT("COMMON_CONFLICT", "요청 상태가 충돌합니다.", 409),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
