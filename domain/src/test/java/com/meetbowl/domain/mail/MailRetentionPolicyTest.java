package com.meetbowl.domain.mail;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class MailRetentionPolicyTest {

    @Test
    void create_success() {
        MailRetentionPolicy policy =
                new MailRetentionPolicy(
                        UUID.randomUUID(),
                        365,
                        365,
                        30,
                        true,
                        UUID.randomUUID(),
                        Instant.parse("2026-06-08T08:00:00Z"));

        assertTrue(policy.autoDeleteEnabled());
    }

    @Test
    void create_fail_when_retention_days_invalid() {
        assertThrows(
                BusinessException.class,
                () ->
                        new MailRetentionPolicy(
                                UUID.randomUUID(),
                                0,
                                365,
                                30,
                                true,
                                UUID.randomUUID(),
                                Instant.parse("2026-06-08T08:00:00Z")));
    }
}
