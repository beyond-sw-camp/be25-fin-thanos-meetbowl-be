package com.meetbowl.common.exception;

import com.meetbowl.common.response.ErrorDetail;
import java.util.List;

/**
 * 도메인/Application 계층에서 의도한 실패를 표현하는 예외다.
 * Controller는 이 예외를 직접 처리하지 않고 GlobalExceptionHandler에 맡긴다.
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
        // details는 외부에서 변경되지 않도록 복사해 응답 생성 시점까지 동일하게 유지한다.
        this.details = List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
