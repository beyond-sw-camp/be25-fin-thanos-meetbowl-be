package com.meetbowl.application.user;

public record MySettingsResult(
        int meetingStartReminderMinutes,
        int minutesReviewReminderMinutes,
        boolean autoBackupEnabled) {}
