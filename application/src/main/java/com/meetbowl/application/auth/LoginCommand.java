package com.meetbowl.application.auth;

public record LoginCommand(String loginId, String password, String ipAddress, String userAgent) {}
