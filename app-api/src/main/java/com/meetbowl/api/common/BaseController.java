package com.meetbowl.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.meetbowl.common.response.ApiResponse;

/** Controller가 응답 외피를 직접 조립하면서 공통 API 계약을 벗어나지 않도록 성공 응답 생성 지점을 제한한다. */
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
