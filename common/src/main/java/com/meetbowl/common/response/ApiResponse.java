package com.meetbowl.common.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meetbowl.common.exception.ErrorCode;

/** 모든 REST API가 사용하는 최상위 응답 포맷이다. 성공 시에는 data/message만, 실패 시에는 error만 내려가도록 null 필드는 직렬화에서 제외한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, ErrorResponse error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.message());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return fail(errorCode, message, List.of());
    }

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
