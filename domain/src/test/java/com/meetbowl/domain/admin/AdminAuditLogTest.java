package com.meetbowl.domain.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class AdminAuditLogTest {

    @Test
    void create_success() {
        AdminAuditLog log =
                new AdminAuditLog(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "관리자",
                        "USER",
                        UUID.randomUUID(),
                        "AUTH",
                        "ROLE_CHANGE",
                        AuditResult.SUCCESS,
                        "{\"role\":\"USER\"}",
                        "{\"role\":\"ADMIN\"}",
                        "127.0.0.1",
                        "JUnit",
                        Instant.parse("2026-06-08T08:00:00Z"));

        assertEquals(AuditResult.SUCCESS, log.result());
    }

    @Test
    void create_fail_when_actor_name_missing() {
        assertThrows(
                BusinessException.class,
                () ->
                        new AdminAuditLog(
                                null,
                                UUID.randomUUID(),
                                " ",
                                "USER",
                                UUID.randomUUID(),
                                "AUTH",
                                "ROLE_CHANGE",
                                AuditResult.SUCCESS,
                                null,
                                null,
                                null,
                                null,
                                Instant.parse("2026-06-08T08:00:00Z")));
    }
}
