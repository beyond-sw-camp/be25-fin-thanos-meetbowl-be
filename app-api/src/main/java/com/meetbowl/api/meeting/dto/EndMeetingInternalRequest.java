package com.meetbowl.api.meeting.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/** STT 서버 같은 내부 시스템이 회의 종료를 API 서버에 알릴 때 사용하는 요청 DTO다. */
public record EndMeetingInternalRequest(
        Instant endedAt,
        UUID correlationId,
        @NotBlank(message = "reason은 필수입니다.") String reason,
        @NotBlank(message = "triggeredBy는 필수입니다.") String triggeredBy) {}
