package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

public record LogoutCommand(
        UUID userId, String refreshToken, String accessTokenId, Instant accessTokenExpiresAt) {}
