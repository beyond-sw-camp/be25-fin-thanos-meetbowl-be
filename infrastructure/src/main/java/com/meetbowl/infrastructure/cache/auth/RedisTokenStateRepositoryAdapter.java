package com.meetbowl.infrastructure.cache.auth;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@Repository
public class RedisTokenStateRepositoryAdapter implements TokenStateRepositoryPort {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String REVOKED_ACCESS_TOKEN_KEY_PREFIX = "auth:revoked-access:";
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
    }

    @Override
    public Optional<UUID> consumeRefreshToken(String tokenHash) {
        String userId = redisTemplate.opsForValue().getAndDelete(refreshTokenKey(tokenHash));
        return Optional.ofNullable(userId).map(UUID::fromString);
    }

    @Override
    public boolean deleteRefreshToken(String tokenHash, UUID userId) {
        Long deleted =
                redisTemplate.execute(
                        DELETE_IF_VALUE_MATCHES,
                        List.of(refreshTokenKey(tokenHash)),
                        userId.toString());
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

    private String refreshTokenKey(String tokenHash) {
        return REFRESH_TOKEN_KEY_PREFIX + tokenHash;
    }

    private String revokedAccessTokenKey(String tokenId) {
        return REVOKED_ACCESS_TOKEN_KEY_PREFIX + tokenId;
    }
}
