package com.meetbowl.common.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 클라이언트가 엔드포인트마다 다른 응답 형태를 해석하지 않도록 REST 응답의 최상위 계약을 이 타입으로 고정한다.
 * null 필드 제외 정책도 이 계약의 일부이며, 성공 응답에 error가 섞이거나 실패 응답에 data가 노출되는 것을 막는다.
 */
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
