package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 개인 일정 등록 요청 DTO다. 시간 순서 같은 업무 규칙은 도메인이 검증하고 여기서는 필수값만 확인한다. */
public record CreateCalendarEventRequest(
        @NotBlank(message = "일정 제목은 필수입니다.") String title,
        String description,
        @NotNull(message = "일정 시작 시각은 필수입니다.") Instant startedAt,
        @NotNull(message = "일정 종료 시각은 필수입니다.") Instant endedAt,
        boolean allDay) {}
