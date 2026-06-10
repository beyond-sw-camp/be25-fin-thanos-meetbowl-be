package com.meetbowl.api.personalworkspace.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCalendarEventRequest(
        @NotBlank(message = "일정 제목은 필수입니다.") String title,
        String description,
        @NotNull(message = "일정 시작 시각은 필수입니다.") Instant startedAt,
        @NotNull(message = "일정 종료 시각은 필수입니다.") Instant endedAt,
        boolean allDay) {}
