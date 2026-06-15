package com.meetbowl.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class UserSettingTest {

    @Test
    void createDefault_success() {
        UserSetting setting =
                UserSetting.createDefault(UUID.randomUUID(), Instant.parse("2026-06-08T08:00:00Z"));

        assertEquals(
                UserSetting.DEFAULT_MEETING_REMINDER_MINUTES_BEFORE,
                setting.meetingReminderMinutesBefore());
        assertEquals(
                UserSetting.DEFAULT_MINUTES_REVIEW_REMINDER_MINUTES,
                setting.minutesReviewReminderMinutes());
        assertEquals(UserSetting.DEFAULT_AUTO_BACKUP_TIME, setting.autoBackupTime());
        assertFalse(setting.autoBackupEnabled());
    }

    @Test
    void create_failsWhenMinutesReviewReminderMinutesIsNotAllowed() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                new UserSetting(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        10,
                                        90,
                                        false,
                                        LocalTime.of(18, 0),
                                        Instant.parse("2026-06-08T08:00:00Z"),
                                        Instant.parse("2026-06-08T08:00:00Z")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }
}
