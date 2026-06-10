package com.meetbowl.api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionInternalTokenValidatorTest {

    @Test
    void validate_rejects_local_default_token() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ProductionInternalTokenValidator.validate(
                                "meetbowl-local-internal-token-32bytes"));
    }

    @Test
    void validate_rejects_documented_example_token() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        ProductionInternalTokenValidator.validate(
                                "change-me-to-a-secure-internal-token"));
    }

    @Test
    void validate_rejects_token_shorter_than_32_bytes() {
        assertThrows(
                IllegalStateException.class,
                () -> ProductionInternalTokenValidator.validate("short-internal-token"));
    }

    @Test
    void validate_accepts_non_public_token_of_at_least_32_bytes() {
        assertDoesNotThrow(
                () ->
                        ProductionInternalTokenValidator.validate(
                                "production-only-internal-token-with-at-least-32-bytes"));
    }
}
