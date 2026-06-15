package com.meetbowl.application.mail;

import java.util.UUID;

public record MailRetentionPolicyCommand(
        int retentionDays,
        boolean autoDeleteEnabled,
        UUID adminId,
        String adminName,
        String ipAddress,
        String userAgent) {}
