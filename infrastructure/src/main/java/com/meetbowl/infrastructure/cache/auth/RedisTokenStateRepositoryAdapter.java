package com.meetbowl.infrastructure.cache.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@Repository
public class RedisTokenStateRepositoryAdapter implements TokenStateRepositoryPort {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_TOKEN_KEY_PREFIX = "auth:user-refresh:";
    private static final String REVOKED_ACCESS_TOKEN_KEY_PREFIX = "auth:revoked-access:";
    private static final String USER_SESSIONS_REVOKED_AT_KEY_PREFIX = "auth:user-revoked-at:";
    private static final DefaultRedisScript<Long> DELETE_IF_VALUE_MATCHES =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStateRepositoryAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveRefreshToken(String tokenHash, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(refreshTokenKey(tokenHash), userId.toString(), ttl);
        redisTemplate.opsForSet().add(userRefreshTokenKey(userId), tokenHash);
        redisTemplate.expire(userRefreshTokenKey(userId), ttl);
    }

    @Override
    public Optional<UUID> consumeRefreshToken(String tokenHash) {
        String userId = redisTemplate.opsForValue().getAndDelete(refreshTokenKey(tokenHash));
        Optional<UUID> consumedUserId = Optional.ofNullable(userId).map(UUID::fromString);
        consumedUserId.ifPresent(
                id -> redisTemplate.opsForSet().remove(userRefreshTokenKey(id), tokenHash));
        return consumedUserId;
    }

    @Override
    public boolean deleteRefreshToken(String tokenHash, UUID userId) {
        Long deleted =
                redisTemplate.execute(
                        DELETE_IF_VALUE_MATCHES,
                        List.of(refreshTokenKey(tokenHash)),
                        userId.toString());
        if (deleted != null && deleted > 0) {
            redisTemplate.opsForSet().remove(userRefreshTokenKey(userId), tokenHash);
        }
        return deleted != null && deleted > 0;
    }

    @Override
    public void revokeAccessToken(String tokenId, Duration ttl) {
        redisTemplate.opsForValue().set(revokedAccessTokenKey(tokenId), "revoked", ttl);
    }

    @Override
    public boolean isAccessTokenRevoked(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(revokedAccessTokenKey(tokenId)));
    }

    @Override
    public void revokeUserSessions(UUID userId, Instant revokedAt) {
        // 사용자별 refresh token 인덱스로 모든 재발급 경로를 제거하고,
        // 기존 access token 은 발급 시각 기준으로 차단한다.
        String userRefreshTokenKey = userRefreshTokenKey(userId);
        Set<String> tokenHashes = redisTemplate.opsForSet().members(userRefreshTokenKey);
        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            redisTemplate.delete(tokenHashes.stream().map(this::refreshTokenKey).toList());
        }
        redisTemplate.delete(userRefreshTokenKey);
        redisTemplate
                .opsForValue()
                .set(userSessionsRevokedAtKey(userId), Long.toString(revokedAt.toEpochMilli()));
    }

    @Override
    public boolean isUserSessionRevoked(UUID userId, Instant accessTokenIssuedAt) {
        String revokedAtValue = redisTemplate.opsForValue().get(userSessionsRevokedAtKey(userId));
        if (revokedAtValue == null) {
            return false;
        }
        Instant revokedAt = Instant.ofEpochMilli(Long.parseLong(revokedAtValue));
        // JWT iat 는 초 단위여서, 같은 초에 재로그인한 새 토큰까지 막지 않도록 초 단위로만 비교한다.
        return accessTokenIssuedAt.getEpochSecond() < revokedAt.getEpochSecond();
    }

    private String refreshTokenKey(String tokenHash) {
        return REFRESH_TOKEN_KEY_PREFIX + tokenHash;
    }

    private String userRefreshTokenKey(UUID userId) {
        return USER_REFRESH_TOKEN_KEY_PREFIX + userId;
    }

    private String revokedAccessTokenKey(String tokenId) {
        return REVOKED_ACCESS_TOKEN_KEY_PREFIX + tokenId;
    }

    private String userSessionsRevokedAtKey(UUID userId) {
        return USER_SESSIONS_REVOKED_AT_KEY_PREFIX + userId;
    }
}
