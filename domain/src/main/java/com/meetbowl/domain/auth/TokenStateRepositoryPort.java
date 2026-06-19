package com.meetbowl.domain.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenStateRepositoryPort {

    void saveRefreshToken(String tokenHash, UUID userId, Duration ttl);

    Optional<UUID> consumeRefreshToken(String tokenHash);

    boolean deleteRefreshToken(String tokenHash, UUID userId);

    void revokeAccessToken(String tokenId, Duration ttl);

    boolean isAccessTokenRevoked(String tokenId);

    void revokeUserSessions(UUID userId, Instant revokedAt);

    boolean isUserSessionRevoked(UUID userId, Instant accessTokenIssuedAt);
}
