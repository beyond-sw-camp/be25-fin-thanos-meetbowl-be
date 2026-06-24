package com.meetbowl.application.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.domain.meeting.AttendeeConflict;

/**
 * 참석자 시간 겹침 검사 출력 모델이다. 겹친 (사용자, 회의) 한 건을 나타내며, 어떤 회의와 겹쳤는지 보여줄 수 있도록 회의 제목/시각을 함께 담는다. 사용자 이름 등
 * 상세는 app-api/FE가 사용자 도메인으로 별도 조회한다.
 */
public record AttendeeConflictResult(
        UUID userId,
        UUID meetingId,
        String meetingTitle,
        Instant scheduledAt,
        Instant scheduledEndAt) {

    public static AttendeeConflictResult of(AttendeeConflict conflict) {
        return new AttendeeConflictResult(
                conflict.userId(),
                conflict.meetingId(),
                conflict.meetingTitle(),
                conflict.scheduledAt(),
                conflict.scheduledEndAt());
    }
}
