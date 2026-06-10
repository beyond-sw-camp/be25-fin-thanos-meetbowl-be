package com.meetbowl.common.response;

import java.util.List;

/** 문서의 공통 실패 응답 error 객체와 1:1로 맞춘다. */
public record ErrorResponse(String code, String message, List<ErrorDetail> details) {}
