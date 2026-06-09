package com.meetbowl.api.minutes;

import jakarta.validation.constraints.NotBlank;

/** 회의록 검토 및 수정 API 요청이다. 현재 회의록 도메인이 보관하는 요약 본문만 수정한다. */
public record ReviseMinutesRequest(@NotBlank(message = "회의록 요약은 필수입니다.") String summary) {}
