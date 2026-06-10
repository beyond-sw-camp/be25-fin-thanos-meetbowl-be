package com.meetbowl.common.response;

import java.util.List;

/** 실패 응답의 구조를 중앙 계약으로 유지해 기능별 오류 표현이 제각각 확장되는 것을 막는다. */
public record ErrorResponse(String code, String message, List<ErrorDetail> details) {}
