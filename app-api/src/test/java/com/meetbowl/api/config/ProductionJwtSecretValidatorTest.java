package com.meetbowl.api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionJwtSecretValidatorTest {

    @Test
    void validate_rejects_local_default_secret() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ProductionJwtSecretValidator.validate(
                                "meetbowl-local-development-secret-key-32bytes"));
    }

    @Test
    void validate_rejects_documented_example_secret() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ProductionJwtSecretValidator.validate(
                                "change-me-to-a-secure-256-bit-secret"));
    }

    @Test
    void validate_rejects_secret_shorter_than_32_bytes() {
        assertThrows(
                IllegalStateException.class,
                () -> ProductionJwtSecretValidator.validate("short-production-secret"));
    }

    @Test
    void validate_accepts_non_public_secret_of_at_least_32_bytes() {
        assertDoesNotThrow(
                () ->
                        ProductionJwtSecretValidator.validate(
                                "production-only-secret-value-with-at-least-32-bytes"));
    }
}
