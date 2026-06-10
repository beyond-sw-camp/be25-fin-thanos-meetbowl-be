package com.meetbowl.application.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meetbowl.domain.auth.JwtTokenProvider;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;

@Service
public class AuthTokenIssuer {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final String INITIAL_PASSWORD_CHANGE_REQUIRED_CLAIM =
            "initialPasswordChangeRequired";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final long refreshTokenExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenIssuer(
            JwtTokenProvider jwtTokenProvider,
            TokenStateRepositoryPort tokenStateRepositoryPort,
            @Value("${meetbowl.security.refresh-token.expiration-seconds:1209600}")
                    long refreshTokenExpirationSeconds) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public IssuedTokens issue(User user) {
        return issue(user, true);
    }

    public IssuedTokens issueInitialPasswordChangeToken(User user) {
        return issue(user, false);
    }

    private IssuedTokens issue(User user, boolean issueRefreshToken) {
        String accessTokenId = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.role().name());
        claims.put("displayName", user.name());
        claims.put(INITIAL_PASSWORD_CHANGE_REQUIRED_CLAIM, user.initialPasswordChangeRequired());
        if (user.affiliateId() != null) {
            claims.put("organizationId", user.affiliateId().toString());
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(
                        user.id().toString(), accessTokenId, Map.copyOf(claims));

        if (!issueRefreshToken) {
            return new IssuedTokens(
                    accessToken,
                    null,
                    "Bearer",
                    jwtTokenProvider.getAccessTokenExpirationSeconds(),
                    0L);
        }

        String refreshToken = createRefreshToken();
        tokenStateRepositoryPort.saveRefreshToken(
                RefreshTokenHasher.hash(refreshToken),
                user.id(),
                Duration.ofSeconds(refreshTokenExpirationSeconds));

        return new IssuedTokens(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                refreshTokenExpirationSeconds);
    }

    private String createRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
