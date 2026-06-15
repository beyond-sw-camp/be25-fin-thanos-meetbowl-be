package com.meetbowl.application.mail;

import java.time.Instant;
import java.util.UUID;

public record MailRetentionPolicyResult(
        int retentionDays, boolean autoDeleteEnabled, Instant updatedAt, UUID updatedBy) {}
