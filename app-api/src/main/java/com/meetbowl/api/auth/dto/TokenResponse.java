package com.meetbowl.api.auth.dto;

import com.meetbowl.application.auth.IssuedTokens;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn) {

    public static TokenResponse from(IssuedTokens tokens) {
        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.accessTokenExpiresIn(),
                tokens.refreshTokenExpiresIn());
    }
}
