package com.meetbowl.common.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meetbowl.common.exception.ErrorCode;

/** 모든 REST API가 사용하는 최상위 응답 포맷이다. 성공 시에는 data/message만, 실패 시에는 error만 내려가도록 null 필드는 직렬화에서 제외한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, ErrorResponse error) {

    /** 조회/생성/수정 결과처럼 응답 본문이 있는 성공 응답에 사용한다. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /** 성공 메시지를 명시적으로 내려야 하는 API에만 사용한다. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /** 삭제, 로그아웃 등 별도 응답 본문이 필요 없는 성공 응답에 사용한다. */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    /** 대부분의 비즈니스 예외 응답은 ErrorCode의 기본 메시지를 그대로 사용한다. */
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.message());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return fail(errorCode, message, List.of());
    }

    /** validation처럼 필드별 실패 사유가 있는 경우 details를 함께 내려준다. */
    public static ApiResponse<Void> fail(ErrorCode errorCode, List<ErrorDetail> details) {
        return fail(errorCode, errorCode.message(), details);
    }

    public static ApiResponse<Void> fail(
            ErrorCode errorCode, String message, List<ErrorDetail> details) {
        return fail(errorCode.code(), message, details);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return fail(code, message, List.of());
    }

    public static ApiResponse<Void> fail(String code, String message, List<ErrorDetail> details) {
        return new ApiResponse<>(false, null, null, new ErrorResponse(code, message, details));
    }
}
