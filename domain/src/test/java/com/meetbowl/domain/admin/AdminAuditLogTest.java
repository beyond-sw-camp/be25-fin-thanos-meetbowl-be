package com.meetbowl.domain.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;

class AdminAuditLogTest {

    @Test
    void createSuccess() {
        AdminAuditLog log =
                new AdminAuditLog(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "관리자",
                        "USER",
                        UUID.randomUUID(),
                        "user01",
                        "홍길동",
                        "AUTH",
                        "ROLE_CHANGE",
                        AuditResult.SUCCESS,
                        "{\"role\":\"USER\"}",
                        "{\"role\":\"ADMIN\"}",
                        "127.0.0.1",
                        "JUnit",
                        Instant.parse("2026-06-08T08:00:00Z"));

        assertEquals(AuditResult.SUCCESS, log.result());
        assertEquals("user01", log.targetLoginId());
        assertEquals("홍길동", log.targetName());
    }

    @Test
    void createFailsWhenActorNameMissing() {
        assertThrows(
                BusinessException.class,
                () ->
                        new AdminAuditLog(
                                null,
                                UUID.randomUUID(),
                                " ",
                                "USER",
                                UUID.randomUUID(),
                                null,
                                null,
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
