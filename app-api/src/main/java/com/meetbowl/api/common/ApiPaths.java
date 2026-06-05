package com.meetbowl.api.common;

/**
 * REST API 공통 경로 규칙을 모아둔다.
 * 새 Controller는 API_V1을 기준으로 리소스 경로만 덧붙인다.
 */
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    private ApiPaths() {
    }
}
