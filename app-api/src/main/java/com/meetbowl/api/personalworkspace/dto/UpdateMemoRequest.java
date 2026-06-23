package com.meetbowl.api.personalworkspace.dto;

import jakarta.validation.constraints.NotBlank;

/** 개인 메모 수정 요청 본문이다. 제목·내용을 필수로 받는다. */
public record UpdateMemoRequest(
        @NotBlank(message = "메모 제목은 필수입니다.") String title,
        @NotBlank(message = "메모 내용은 필수입니다.") String content) {}
