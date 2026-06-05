package com.meetbowl.common.response;

import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ErrorResponse error
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return fail(code, message, List.of());
    }

    public static ApiResponse<Void> fail(String code, String message, List<ErrorDetail> details) {
        return new ApiResponse<>(false, null, null, new ErrorResponse(code, message, details));
    }
}
