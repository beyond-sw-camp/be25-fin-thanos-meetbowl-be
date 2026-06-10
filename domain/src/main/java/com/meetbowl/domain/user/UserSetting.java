package com.meetbowl.domain.user;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record UserSetting(
        UUID id,
        UUID userId,
        int meetingReminderMinutesBefore,
        boolean autoBackupEnabled,
        LocalTime autoBackupTime,
        Instant createdAt,
        Instant updatedAt) {

    public static final int DEFAULT_MEETING_REMINDER_MINUTES_BEFORE = 10;
    public static final LocalTime DEFAULT_AUTO_BACKUP_TIME = LocalTime.of(18, 0);

    public UserSetting {
        if (userId == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (meetingReminderMinutesBefore < 0) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "회의 알림 시간은 음수일 수 없습니다.");
        }
        if (autoBackupEnabled && autoBackupTime == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "자동 백업 시각은 필수입니다.");
        }
    }

    public static UserSetting createDefault(UUID userId, Instant now) {
        return new UserSetting(
                null,
                userId,
                DEFAULT_MEETING_REMINDER_MINUTES_BEFORE,
                false,
                DEFAULT_AUTO_BACKUP_TIME,
                now,
                now);
    }
}
