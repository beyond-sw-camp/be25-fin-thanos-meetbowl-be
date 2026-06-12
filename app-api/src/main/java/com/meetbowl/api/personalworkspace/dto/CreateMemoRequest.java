package com.meetbowl.api.personalworkspace.dto;

import jakarta.validation.constraints.NotBlank;

/** 개인 메모 작성 요청 DTO다. 길이 제한 등 세부 규칙은 도메인이 검증한다. */
public record CreateMemoRequest(
        @NotBlank(message = "메모 제목은 필수입니다.") String title,
        @NotBlank(message = "메모 내용은 필수입니다.") String content) {}
