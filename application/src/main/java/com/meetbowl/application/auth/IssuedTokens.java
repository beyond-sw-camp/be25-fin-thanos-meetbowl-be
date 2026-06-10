package com.meetbowl.application.auth;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn) {}
