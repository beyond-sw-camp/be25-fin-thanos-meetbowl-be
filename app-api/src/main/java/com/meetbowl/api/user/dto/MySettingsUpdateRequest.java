package com.meetbowl.api.user.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MySettingsUpdateRequest(
        @NotNull @Min(0) Integer meetingStartReminderMinutes,
        @NotNull @Min(1) Integer minutesReviewReminderMinutes,
        @NotNull Boolean autoBackupEnabled,
        LocalTime autoBackupTime) {}
