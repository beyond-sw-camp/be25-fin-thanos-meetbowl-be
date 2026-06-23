package com.meetbowl.domain.user;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record UserSetting(
        UUID id,
        UUID userId,
        int meetingReminderMinutesBefore,
        // 회의록 검토 전까지 반복 알림을 보내는 주기이며, 분 단위 정수로 저장한다.
        int minutesReviewReminderMinutes,
        Instant createdAt,
        Instant updatedAt) {

    public static final int DEFAULT_MEETING_REMINDER_MINUTES_BEFORE = 10;
    public static final int DEFAULT_MINUTES_REVIEW_REMINDER_MINUTES = 60;
    public static final Set<Integer> ALLOWED_MINUTES_REVIEW_REMINDER_MINUTES =
            Set.of(60, 120, 180, 240);

    public UserSetting {
        if (userId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (meetingReminderMinutesBefore < 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 알림 시간은 음수일 수 없습니다.");
        }
        // 회의록 미검토 알림은 현재 계약상 1~4시간 주기만 저장한다.
        if (!ALLOWED_MINUTES_REVIEW_REMINDER_MINUTES.contains(minutesReviewReminderMinutes)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "회의록 미검토 알림 주기가 올바르지 않습니다.");
        }
    }

    public static UserSetting createDefault(UUID userId, Instant now) {
        return new UserSetting(
                null,
                userId,
                DEFAULT_MEETING_REMINDER_MINUTES_BEFORE,
                // 개인 설정이 아직 없는 사용자는 1시간마다 알림을 받도록 기본값을 둔다.
                DEFAULT_MINUTES_REVIEW_REMINDER_MINUTES,
                now,
                now);
    }
}
