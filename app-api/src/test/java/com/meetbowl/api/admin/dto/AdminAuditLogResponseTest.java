package com.meetbowl.api.admin.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.AdminAuditLogResult;

class AdminAuditLogResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void fromMapsActionAndTargetLabelsAndFormatsUserChanges() {
        AdminAuditLogResponse response =
                AdminAuditLogResponse.from(
                        new AdminAuditLogResult(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "admin01",
                                "USER_UPDATE",
                                "USER",
                                UUID.randomUUID(),
                                "SUCCESS",
                                null,
                                "{\"status\":\"ACTIVE\",\"activeFrom\":1781654400,\"activeUntil\":null}",
                                "{\"status\":\"INACTIVE\",\"activeFrom\":1781827200,\"activeUntil\":1782000000}",
                                Instant.parse("2026-06-23T00:00:00Z")),
                        OBJECT_MAPPER);

        assertEquals("회원 수정", response.actionLabel());
        assertEquals("회원", response.targetTypeLabel());
        assertEquals("회원 수정", response.displayTitle());
        assertEquals(3, response.displayChangeItems().size());
        assertEquals("상태", response.displayChangeItems().get(0).label());
        assertEquals("활성", response.displayChangeItems().get(0).beforeValue());
        assertEquals("비활성", response.displayChangeItems().get(0).afterValue());
        assertEquals("활성 시작일", response.displayChangeItems().get(1).label());
        assertEquals("2026.06.17", response.displayChangeItems().get(1).beforeValue());
        assertEquals("2026.06.19", response.displayChangeItems().get(1).afterValue());
        assertEquals("활성 종료일", response.displayChangeItems().get(2).label());
        assertEquals("-", response.displayChangeItems().get(2).beforeValue());
        assertEquals("2026.06.21", response.displayChangeItems().get(2).afterValue());
    }

    @Test
    void fromFormatsExcelImportSummaryWithoutRawJson() {
        AdminAuditLogResponse response =
                AdminAuditLogResponse.from(
                        new AdminAuditLogResult(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "admin01",
                                "ORGANIZATION_MEMBER_EXCEL_IMPORT",
                                "ORGANIZATION_MEMBER_EXCEL",
                                null,
                                "FAILED",
                                "{\"message\":\"raw\"}",
                                null,
                                """
                                {"fileName":"meetbowl_organization_members_template_v2.xlsx","message":"필수값이 비어 있습니다.","errorCount":3}
                                """,
                                Instant.parse("2026-06-23T00:00:00Z")),
                        OBJECT_MAPPER);

        assertEquals("조직/회원 엑셀 업로드", response.actionLabel());
        assertEquals("조직/회원 엑셀", response.targetTypeLabel());
        assertEquals(
                List.of("파일명", "처리 결과", "실패 사유", "오류 건수"),
                response.displayChangeItems().stream()
                        .map(AdminAuditLogResponse.DisplayChangeItemResponse::label)
                        .toList());
        assertEquals(
                "meetbowl_organization_members_template_v2.xlsx",
                response.displayChangeItems().get(0).value());
        assertEquals("실패", response.displayChangeItems().get(1).value());
    }

    @Test
    void fromFallsBackToRawValuesWhenMappingDoesNotExist() {
        AdminAuditLogResponse response =
                AdminAuditLogResponse.from(
                        new AdminAuditLogResult(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "admin01",
                                "CUSTOM_ACTION",
                                "CUSTOM_TARGET",
                                UUID.randomUUID(),
                                "SUCCESS",
                                null,
                                null,
                                "{\"message\":\"custom\"}",
                                Instant.parse("2026-06-23T00:00:00Z")),
                        OBJECT_MAPPER);

        assertEquals("CUSTOM_ACTION", response.actionLabel());
        assertEquals("CUSTOM_TARGET", response.targetTypeLabel());
        assertEquals("메시지", response.displayChangeItems().get(0).label());
        assertEquals("custom", response.displayChangeItems().get(0).value());
    }
}
