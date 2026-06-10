package com.meetbowl.api.personalworkspace.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemoRequest(
        @NotBlank(message = "메모 제목은 필수입니다.") String title,
        @NotBlank(message = "메모 내용은 필수입니다.") String content) {}
