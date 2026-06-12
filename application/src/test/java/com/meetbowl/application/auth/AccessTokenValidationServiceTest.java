package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidationServiceTest {

    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;

    @Test
    void revokedTokenIdIsRejected() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        given(tokenStateRepositoryPort.isAccessTokenRevoked("token-id")).willReturn(true);

        AccessTokenValidationService service =
                new AccessTokenValidationService(tokenStateRepositoryPort);

        assertTrue(service.isRevoked("token-id", userId, issuedAt));
    }

    @Test
    void tokenIssuedBeforeUserSessionRevocationIsRejected() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        given(tokenStateRepositoryPort.isAccessTokenRevoked("token-id")).willReturn(false);
        given(tokenStateRepositoryPort.isUserSessionRevoked(userId, issuedAt)).willReturn(true);

        AccessTokenValidationService service =
                new AccessTokenValidationService(tokenStateRepositoryPort);

        assertTrue(service.isRevoked("token-id", userId, issuedAt));
    }

    @Test
    void activeTokenIsAccepted() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now();

        AccessTokenValidationService service =
                new AccessTokenValidationService(tokenStateRepositoryPort);

        assertFalse(service.isRevoked("token-id", userId, issuedAt));
    }
}
