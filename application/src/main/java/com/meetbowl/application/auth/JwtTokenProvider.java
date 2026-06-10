package com.meetbowl.application.auth;

import java.util.Map;

public interface JwtTokenProvider {
    String createToken(String subject, Map<String, Object> claims);

    long getExpirationSeconds();
}
