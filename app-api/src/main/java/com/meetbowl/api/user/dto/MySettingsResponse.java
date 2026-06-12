package com.meetbowl.api.user.dto;

import java.time.LocalTime;

import com.meetbowl.application.user.MySettingsResult;

public record MySettingsResponse(
        int meetingStartReminderMinutes, boolean autoBackupEnabled, LocalTime autoBackupTime) {

    public static MySettingsResponse from(MySettingsResult result) {
        return new MySettingsResponse(
                result.meetingStartReminderMinutes(),
                result.autoBackupEnabled(),
                result.autoBackupTime());
    }
}
