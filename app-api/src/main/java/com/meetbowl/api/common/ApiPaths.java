package com.meetbowl.api.common;

/** 버전 경로가 Controller별 문자열로 흩어져 일부 API만 다른 계약으로 노출되는 것을 막는다. */
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    private ApiPaths() {}
}
