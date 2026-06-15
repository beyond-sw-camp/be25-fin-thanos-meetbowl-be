package com.meetbowl.application.user;

import java.time.LocalTime;

public record MySettingsResult(
        int meetingStartReminderMinutes,
        int minutesReviewReminderMinutes,
        boolean autoBackupEnabled,
        LocalTime autoBackupTime) {}
