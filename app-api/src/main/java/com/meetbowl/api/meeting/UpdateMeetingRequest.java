package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 회의 수정 요청 DTO다. 제목·예정 시작/종료·회의실·참석자·검토자를 변경한다. {@code meetingRoomId}가 null이면 회의실을 비우고 화상회의만 진행한다.
 * 참석자·검토자는 생성과 동일 규칙으로 전체 교체되며(참석자 최소 1명 필수, 검토자는 참석자 중 1명), 주최자는 인증 토큰에서 가져오므로 본문에 받지 않는다.
 */
public record UpdateMeetingRequest(
        @NotBlank(message = "회의 제목은 필수입니다.") String title,
        @NotNull(message = "예정 시작 시각은 필수입니다.") Instant scheduledAt,
        @NotNull(message = "예정 종료 시각은 필수입니다.") Instant scheduledEndAt,
        UUID meetingRoomId,
        @NotEmpty(message = "참여자는 최소 1명 이상 선택해야 합니다.") List<UUID> attendeeUserIds,
        @NotNull(message = "회의록 검토자는 필수입니다.") UUID reviewerUserId,
        String description) {}
