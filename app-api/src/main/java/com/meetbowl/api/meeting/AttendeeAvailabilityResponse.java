package com.meetbowl.api.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.meetbowl.application.meeting.AttendeeConflictResult;

/**
 * 참석자 시간 겹침 검사 응답 DTO다. {@code conflicts}가 비어 있으면 겹치는 사용자가 없다는 뜻이다. 한 사용자가 여러 회의와 겹치면 회의마다 한 건씩
 * 들어오므로, 프론트는 userId로 묶어 사용자당 한 번만 경고("○○님은 이미 회의 참석 중입니다")를 표시한다. 사용자 이름은 FE가 userId로 별도 조회한다.
 */
public record AttendeeAvailabilityResponse(List<ConflictItem> conflicts) {

    /** 겹친 (사용자, 회의) 한 건. 어떤 회의와 겹쳤는지 보여줄 수 있도록 회의 제목/시각을 함께 담는다. */
    public record ConflictItem(
            UUID userId,
            UUID meetingId,
            String meetingTitle,
            Instant scheduledAt,
            Instant scheduledEndAt) {

        static ConflictItem from(AttendeeConflictResult conflict) {
            return new ConflictItem(
                    conflict.userId(),
                    conflict.meetingId(),
                    conflict.meetingTitle(),
                    conflict.scheduledAt(),
                    conflict.scheduledEndAt());
        }
    }

    public static AttendeeAvailabilityResponse from(List<AttendeeConflictResult> conflicts) {
        return new AttendeeAvailabilityResponse(
                conflicts.stream().map(ConflictItem::from).toList());
    }
}
