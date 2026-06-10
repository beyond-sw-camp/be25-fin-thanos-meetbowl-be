package com.meetbowl.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserSettingTest {

    @Test
    void createDefault_success() {
        UserSetting setting =
                UserSetting.createDefault(UUID.randomUUID(), Instant.parse("2026-06-08T08:00:00Z"));

        assertEquals(
                UserSetting.DEFAULT_MEETING_REMINDER_MINUTES_BEFORE,
                setting.meetingReminderMinutesBefore());
        assertEquals(UserSetting.DEFAULT_AUTO_BACKUP_TIME, setting.autoBackupTime());
        assertFalse(setting.autoBackupEnabled());
    }
}
