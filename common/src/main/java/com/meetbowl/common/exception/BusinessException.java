package com.meetbowl.common.exception;

import java.util.List;

import com.meetbowl.common.response.ErrorDetail;

/**
 * 도메인/Application 계층에서 의도한 실패를 표현하는 예외다. Controller는 이 예외를 직접 처리하지 않고 GlobalExceptionHandler에 맡긴다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), List.of());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public BusinessException(ErrorCode errorCode, List<ErrorDetail> details) {
        this(errorCode, errorCode.message(), details);
    }

    public BusinessException(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        // 예외가 계층을 통과하는 동안 응답 계약이 호출자 변경에 영향받지 않도록 방어적 복사한다.
        this.details = List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
