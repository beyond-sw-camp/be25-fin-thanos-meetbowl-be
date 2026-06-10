package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

public record ChangeInitialPasswordCommand(
        UUID userId, String newPassword, String accessTokenId, Instant accessTokenExpiresAt) {}
