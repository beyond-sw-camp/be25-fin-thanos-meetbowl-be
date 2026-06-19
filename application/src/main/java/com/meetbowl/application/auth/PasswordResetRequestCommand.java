package com.meetbowl.application.auth;

public record PasswordResetRequestCommand(
        String loginId, String email, String ipAddress, String userAgent) {}
