package com.meetbowl.common.response;

/** validation 실패처럼 특정 입력 필드에 대한 사유를 전달할 때 사용한다. */
public record ErrorDetail(String field, String reason) {}
