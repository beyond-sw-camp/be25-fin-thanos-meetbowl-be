package com.meetbowl.api.minutes;

import jakarta.validation.constraints.NotBlank;

/** 회의록 검토 및 수정 API 요청이다. 목록용 요약과 실제 승인 본문을 함께 수정한다. */
public record ReviseMinutesRequest(
        @NotBlank(message = "회의록 요약은 필수입니다.") String summary,
        @NotBlank(message = "회의록 본문은 필수입니다.") String content) {}
