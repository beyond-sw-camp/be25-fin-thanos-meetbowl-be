package com.meetbowl.api.user.dto;

import com.meetbowl.application.user.MySettingsResult;

public record MySettingsResponse(
        int meetingStartReminderMinutes, int minutesReviewReminderMinutes) {

    public static MySettingsResponse from(MySettingsResult result) {
        return new MySettingsResponse(
                result.meetingStartReminderMinutes(), result.minutesReviewReminderMinutes());
    }
}
