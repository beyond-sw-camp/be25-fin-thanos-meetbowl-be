package com.meetbowl.common.exception;

import java.util.List;

import com.meetbowl.common.response.ErrorDetail;

/**
 * 업무 실패를 HTTP나 Spring 타입과 분리해 하위 계층이 전송 기술에 의존하지 않게 한다.
 * API 경계는 이 정보만 공통 응답으로 변환하므로, 도메인 규칙과 외부 오류 계약을 독립적으로 변경할 수 있다.
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
        this.details = List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
