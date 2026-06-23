package com.meetbowl.api.config;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 프로덕션 환경에서 공개된 예시 값이나 HS256에 부족한 JWT secret 사용을 차단한다. */
@Component
@Profile("prod")
public class ProductionJwtSecretValidator {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Set<String> FORBIDDEN_SECRETS =
            Set.of(
                    "meetbowl-local-development-secret-key-32bytes",
                    "change-me-to-a-secure-256-bit-secret");

    public ProductionJwtSecretValidator(@Value("${meetbowl.security.jwt.secret}") String secret) {
        validate(secret);
    }

    static void validate(String secret) {
        if (secret == null
                || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES
                || FORBIDDEN_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "Production JWT secret must be a non-public value of at least 32 bytes.");
        }
    }
}
