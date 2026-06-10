package com.meetbowl.api.common;

/** 인증·추적 헤더의 철자가 API 진입점마다 달라지는 것을 막기 위한 단일 정의다. */
public final class ApiHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String INTERNAL_TOKEN = "X-Internal-Token";

    private ApiHeaders() {}
}
