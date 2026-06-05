package com.meetbowl.api.common;

/** API 명세서에서 정의한 공통 요청 헤더 이름이다. Controller와 Swagger 설정에서 문자열 중복을 피하기 위해 사용한다. */
public final class ApiHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String INTERNAL_TOKEN = "X-Internal-Token";

    private ApiHeaders() {}
}
