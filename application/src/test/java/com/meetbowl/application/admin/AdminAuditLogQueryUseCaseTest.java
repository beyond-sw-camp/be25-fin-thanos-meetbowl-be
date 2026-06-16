package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AdminAuditLogSearchCondition;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.common.Paged;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogQueryUseCaseTest {

    private static final UUID AUDIT_LOG_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID ACTOR_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final Instant CREATED_AT = Instant.parse("2026-06-12T00:00:00Z");

    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;

    @Test
    void searchSuccessMapsAuditLogAndPaging() {
        AdminAuditLogQueryUseCase useCase =
                new AdminAuditLogQueryUseCase(adminAuditLogRepositoryPort);
        given(adminAuditLogRepositoryPort.findPage(org.mockito.ArgumentMatchers.any()))
                .willReturn(new Paged<>(List.of(log()), 3));

        AdminAuditLogPageResult result =
                useCase.search(
                        new AdminAuditLogSearchCommand(
                                ACTOR_USER_ID,
                                "admin01",
                                "USER_UPDATE",
                                "USER",
                                TARGET_ID,
                                "SUCCESS",
                                Instant.parse("2026-06-01T00:00:00Z"),
                                Instant.parse("2026-06-13T00:00:00Z"),
                                2,
                                2));

        assertEquals(1, result.items().size());
        assertEquals(2, result.page());
        assertEquals(2, result.totalPages());
        assertEquals("USER_UPDATE", result.items().get(0).actionType());
        assertEquals("{\"name\":\"old\"}", result.items().get(0).beforeSnapshot());

        ArgumentCaptor<AdminAuditLogSearchCondition> captor =
                ArgumentCaptor.forClass(AdminAuditLogSearchCondition.class);
        verify(adminAuditLogRepositoryPort).findPage(captor.capture());
        assertEquals(ACTOR_USER_ID, captor.getValue().actorUserId());
        assertEquals("admin01", captor.getValue().actorName());
        assertEquals(AuditResult.SUCCESS, captor.getValue().result());
    }

    @Test
    void getSuccess() {
        AdminAuditLogQueryUseCase useCase =
                new AdminAuditLogQueryUseCase(adminAuditLogRepositoryPort);
        given(adminAuditLogRepositoryPort.findById(AUDIT_LOG_ID)).willReturn(Optional.of(log()));

        AdminAuditLogResult result = useCase.get(AUDIT_LOG_ID);

        assertEquals(AUDIT_LOG_ID, result.auditLogId());
        assertEquals("USER_UPDATE", result.actionType());
        assertEquals(CREATED_AT, result.createdAt());
    }

    @Test
    void getFailsWhenAuditLogDoesNotExist() {
        AdminAuditLogQueryUseCase useCase =
                new AdminAuditLogQueryUseCase(adminAuditLogRepositoryPort);
        given(adminAuditLogRepositoryPort.findById(AUDIT_LOG_ID)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.get(AUDIT_LOG_ID));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void searchFailsWhenResultIsInvalid() {
        AdminAuditLogQueryUseCase useCase =
                new AdminAuditLogQueryUseCase(adminAuditLogRepositoryPort);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.search(
                                        new AdminAuditLogSearchCommand(
                                                null, null, null, null, null, "DONE", null, null, 1,
                                                20)));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    private AdminAuditLog log() {
        return new AdminAuditLog(
                AUDIT_LOG_ID,
                ACTOR_USER_ID,
                "admin01",
                "USER",
                TARGET_ID,
                "USER_MANAGEMENT",
                "UPDATE",
                AuditResult.SUCCESS,
                "{\"name\":\"old\"}",
                "{\"name\":\"new\"}",
                "127.0.0.1",
                "JUnit",
                CREATED_AT);
    }
}
