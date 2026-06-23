package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 회의 수정 입력 모델이다. 제목·예정 시작/종료·회의실·참석자·검토자를 변경한다. {@code requesterId}는 인증된 요청자이며, 주최자가 아니면 수정할 수 없다.
 * {@code meetingRoomId}가 null이면 회의실을 비우고 화상회의만 진행한다. {@code attendeeUserIds}·{@code
 * reviewerUserId}는 생성과 동일 규칙으로 전체 교체된다(참석자 최소 1명, 검토자는 참석자 중 1명).
 */
public record UpdateMeetingCommand(
        UUID meetingId,
        UUID requesterId,
        String title,
        Instant scheduledAt,
        Instant scheduledEndAt,
        UUID meetingRoomId,
        List<UUID> attendeeUserIds,
        UUID reviewerUserId,
        String description) {}
