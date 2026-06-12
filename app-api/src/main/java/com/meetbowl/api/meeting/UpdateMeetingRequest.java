package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 회의 수정 요청 DTO다. 제목·예정 시작/종료·회의실을 변경한다. {@code meetingRoomId}가 null이면 회의실을 비우고 화상회의만 진행한다. 주최자는 인증
 * 토큰에서 가져오므로 본문에 받지 않는다.
 */
public record UpdateMeetingRequest(
        @NotBlank(message = "회의 제목은 필수입니다.") String title,
        @NotNull(message = "예정 시작 시각은 필수입니다.") Instant scheduledAt,
        @NotNull(message = "예정 종료 시각은 필수입니다.") Instant scheduledEndAt,
        UUID meetingRoomId,
        String description) {}
