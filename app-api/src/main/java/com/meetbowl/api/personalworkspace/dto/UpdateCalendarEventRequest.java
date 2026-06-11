package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 개인 일정 수정 요청 본문이다. 제목·시작/종료 시각을 필수로 받고 종일 여부를 함께 받는다. */
public record UpdateCalendarEventRequest(
        @NotBlank(message = "일정 제목은 필수입니다.") String title,
        String description,
        @NotNull(message = "일정 시작 시각은 필수입니다.") Instant startedAt,
        @NotNull(message = "일정 종료 시각은 필수입니다.") Instant endedAt,
        boolean allDay) {}
