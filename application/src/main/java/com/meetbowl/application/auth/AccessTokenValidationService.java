package com.meetbowl.application.auth;

import org.springframework.stereotype.Service;

import com.meetbowl.domain.auth.TokenStateRepositoryPort;

@Service
public class AccessTokenValidationService {

    private final TokenStateRepositoryPort tokenStateRepositoryPort;

    public AccessTokenValidationService(TokenStateRepositoryPort tokenStateRepositoryPort) {
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
    }

    public boolean isRevoked(String tokenId) {
        return tokenStateRepositoryPort.isAccessTokenRevoked(tokenId);
    }
}
