package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.application.mail.MailRetentionPolicyResult;

public record MailRetentionPolicyResponse(
        int retentionDays, boolean autoDeleteEnabled, Instant updatedAt, UUID updatedBy) {

    public static MailRetentionPolicyResponse from(MailRetentionPolicyResult result) {
        return new MailRetentionPolicyResponse(
                result.retentionDays(),
                result.autoDeleteEnabled(),
                result.updatedAt(),
                result.updatedBy());
    }
}
