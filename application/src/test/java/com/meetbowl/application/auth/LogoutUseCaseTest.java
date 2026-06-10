package com.meetbowl.application.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;

    @Test
    void logout_deletes_refresh_token_and_revokes_access_token() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        LogoutUseCase useCase = new LogoutUseCase(tokenStateRepositoryPort);

        useCase.execute(
                new LogoutCommand(
                        userId, refreshToken, "access-token-id", Instant.now().plusSeconds(300)));

        verify(tokenStateRepositoryPort)
                .deleteRefreshToken(RefreshTokenHasher.hash(refreshToken), userId);
        verify(tokenStateRepositoryPort).revokeAccessToken(eq("access-token-id"), any());
    }
}
