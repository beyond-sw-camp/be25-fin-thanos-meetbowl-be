package com.meetbowl.application.admin;

import java.util.UUID;

public record ResetUserPasswordCommand(
        UUID userId, UUID adminId, String adminName, String ipAddress, String userAgent) {}
