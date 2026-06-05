package com.meetbowl.api.sampletask;

import jakarta.validation.constraints.NotBlank;

/** 샘플 API 요청 DTO다. HTTP 검증 전용이며 UseCase에는 Command로 변환해서 전달한다. */
public record CreateSampleTaskRequest(@NotBlank(message = "샘플 작업 제목은 필수입니다.") String title) {}
