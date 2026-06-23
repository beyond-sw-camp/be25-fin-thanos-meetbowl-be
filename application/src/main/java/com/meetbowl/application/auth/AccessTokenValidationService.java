package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@Service
public class AccessTokenValidationService {

    private final TokenStateRepositoryPort tokenStateRepositoryPort;

    public AccessTokenValidationService(TokenStateRepositoryPort tokenStateRepositoryPort) {
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
    }

    public boolean isRevoked(String tokenId, UUID userId, Instant issuedAt) {
        return tokenStateRepositoryPort.isAccessTokenRevoked(tokenId)
                || tokenStateRepositoryPort.isUserSessionRevoked(userId, issuedAt);
    }
}
