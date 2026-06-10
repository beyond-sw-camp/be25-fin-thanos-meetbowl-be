package com.meetbowl.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.meetbowl.common.response.ApiResponse;

/**
 * Controller에서 공통 성공 응답을 짧고 일관되게 만들기 위한 기반 클래스다. 비즈니스 예외는 여기서 처리하지 않고 GlobalExceptionHandler가 담당한다.
 */
public abstract class BaseController {

    protected <T> ApiResponse<T> ok(T data) {
        return ApiResponse.ok(data);
    }

    protected <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.ok(data, message);
    }

    protected ApiResponse<Void> ok() {
        return ApiResponse.ok();
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data));
    }
}
