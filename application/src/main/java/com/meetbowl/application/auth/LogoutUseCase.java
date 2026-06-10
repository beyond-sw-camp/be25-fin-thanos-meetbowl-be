package com.meetbowl.application.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@Service
public class LogoutUseCase {

    private final TokenStateRepositoryPort tokenStateRepositoryPort;

    public LogoutUseCase(TokenStateRepositoryPort tokenStateRepositoryPort) {
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
    }

    public void execute(LogoutCommand command) {
        tokenStateRepositoryPort.deleteRefreshToken(
                RefreshTokenHasher.hash(command.refreshToken()), command.userId());

        Duration remaining = Duration.between(Instant.now(), command.accessTokenExpiresAt());
        if (!remaining.isNegative() && !remaining.isZero()) {
            tokenStateRepositoryPort.revokeAccessToken(command.accessTokenId(), remaining);
        }
    }
}
