package com.meetbowl.api.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MailRetentionPolicyRequest(
        @Min(1) @Max(3650) int retentionDays, boolean autoDeleteEnabled) {}
