package com.meetbowl.application.user;

import java.time.LocalTime;

public record MySettingsResult(
        int meetingStartReminderMinutes, boolean autoBackupEnabled, LocalTime autoBackupTime) {}
