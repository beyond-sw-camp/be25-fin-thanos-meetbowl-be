package com.meetbowl.api.config;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 프로덕션 환경에서 공개된 예시 값이나 짧은 내부 인증 토큰 사용을 차단한다. */
@Component
@Profile("prod")
public class ProductionInternalTokenValidator {

    private static final int MINIMUM_TOKEN_BYTES = 32;
    private static final Set<String> FORBIDDEN_TOKENS =
            Set.of("meetbowl-local-internal-token-32bytes", "change-me-to-a-secure-internal-token");

    public ProductionInternalTokenValidator(
            @Value("${meetbowl.security.internal-token}") String internalToken) {
        validate(internalToken);
    }

    static void validate(String internalToken) {
        if (internalToken == null
                || internalToken.isBlank()
                || internalToken.getBytes(StandardCharsets.UTF_8).length < MINIMUM_TOKEN_BYTES
                || FORBIDDEN_TOKENS.contains(internalToken)) {
            throw new IllegalStateException(
                    "Production internal token must be a non-public value of at least 32 bytes.");
        }
    }
}
