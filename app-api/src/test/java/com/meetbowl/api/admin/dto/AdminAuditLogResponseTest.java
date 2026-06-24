package com.meetbowl.api.admin.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.AdminAuditLogResult;

class AdminAuditLogResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void fromIncludesResolvedIpAddress() {
        AdminAuditLogResponse response =
                AdminAuditLogResponse.from(
                        new AdminAuditLogResult(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "admin01",
                                "USER_UPDATE",
                                "USER",
                                UUID.randomUUID(),
                                "user01",
                                "User One",
                                "203.0.113.10",
                                "SUCCESS",
                                null,
                                "{\"status\":\"ACTIVE\"}",
                                "{\"status\":\"INACTIVE\"}",
                                Instant.parse("2026-06-23T00:00:00Z")),
                        OBJECT_MAPPER);

        assertEquals("203.0.113.10", response.ipAddress());
        assertEquals("user01", response.targetLoginId());
        assertEquals("User One", response.targetName());
        assertNotNull(response.displayTitle());
        assertEquals("ACTIVE", ((java.util.Map<?, ?>) response.beforeSnapshot()).get("status"));
        assertEquals("INACTIVE", ((java.util.Map<?, ?>) response.afterSnapshot()).get("status"));
    }

    @Test
    void fromFallsBackToDashWhenIpAddressIsMissing() {
        AdminAuditLogResponse response =
                AdminAuditLogResponse.from(
                        new AdminAuditLogResult(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "admin01",
                                "CUSTOM_ACTION",
                                "CUSTOM_TARGET",
                                UUID.randomUUID(),
                                null,
                                null,
                                null,
                                "SUCCESS",
                                null,
                                null,
                                "{\"message\":\"custom\"}",
                                Instant.parse("2026-06-23T00:00:00Z")),
                        OBJECT_MAPPER);

        assertEquals("-", response.ipAddress());
        assertEquals("-", response.targetLoginId());
        assertEquals("-", response.targetName());
        assertEquals("custom", response.displayChangeItems().get(0).value());
    }
}
