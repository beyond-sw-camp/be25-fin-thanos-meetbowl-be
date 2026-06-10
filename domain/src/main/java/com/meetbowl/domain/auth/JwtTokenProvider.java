package com.meetbowl.domain.auth;

import java.util.Map;

public interface JwtTokenProvider {

    String createAccessToken(String subject, String tokenId, Map<String, Object> claims);

    long getAccessTokenExpirationSeconds();
}
