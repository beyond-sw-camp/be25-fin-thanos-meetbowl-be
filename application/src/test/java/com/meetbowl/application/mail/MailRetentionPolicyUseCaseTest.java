package com.meetbowl.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.domain.mail.MailRetentionPolicyRepositoryPort;

@ExtendWith(MockitoExtension.class)
class MailRetentionPolicyUseCaseTest {

    private static final UUID POLICY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-06-12T00:00:00Z");

    @Mock private MailRetentionPolicyRepositoryPort mailRetentionPolicyRepositoryPort;
    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;

    @Test
    void getSuccessReturnsDefaultWhenPolicyRowDoesNotExist() {
        MailRetentionPolicyUseCase useCase = useCase();
        given(mailRetentionPolicyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.empty());

        MailRetentionPolicyResult result = useCase.get();

        assertEquals(365, result.retentionDays());
        assertEquals(false, result.autoDeleteEnabled());
        assertEquals(null, result.updatedAt());
        assertEquals(null, result.updatedBy());
    }

    @Test
    void updateSuccessSavesPolicyAndAuditSnapshots() {
        MailRetentionPolicyUseCase useCase = useCase();
        MailRetentionPolicy before = policy(90, false);
        MailRetentionPolicy after =
                new MailRetentionPolicy(POLICY_ID, 180, 180, 180, true, ADMIN_ID, NOW);

        given(mailRetentionPolicyRepositoryPort.findById(POLICY_ID))
                .willReturn(Optional.of(before));
        given(mailRetentionPolicyRepositoryPort.save(any())).willReturn(after);

        MailRetentionPolicyResult result = useCase.update(command(180, true));

        assertEquals(180, result.retentionDays());
        assertEquals(true, result.autoDeleteEnabled());
        assertEquals(NOW, result.updatedAt());
        assertEquals(ADMIN_ID, result.updatedBy());

        ArgumentCaptor<MailRetentionPolicy> policyCaptor =
                ArgumentCaptor.forClass(MailRetentionPolicy.class);
        verify(mailRetentionPolicyRepositoryPort).save(policyCaptor.capture());
        assertEquals(180, policyCaptor.getValue().inboxRetentionDays());
        assertEquals(180, policyCaptor.getValue().sentRetentionDays());
        assertEquals(180, policyCaptor.getValue().trashRetentionDays());
        assertEquals(true, policyCaptor.getValue().autoDeleteEnabled());

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertEquals("MAIL_RETENTION_POLICY", auditLog.targetType());
        assertEquals("UPDATE", auditLog.actionName());
        assertEquals(POLICY_ID, auditLog.targetId());
        assertEquals(NOW, auditLog.occurredAt());
        assertNotNull(auditLog.beforeValue());
        assertNotNull(auditLog.afterValue());
        assertTrue(auditLog.beforeValue().contains("\"retentionDays\":90"));
        assertTrue(auditLog.beforeValue().contains("\"autoDeleteEnabled\":false"));
        assertTrue(auditLog.afterValue().contains("\"retentionDays\":180"));
        assertTrue(auditLog.afterValue().contains("\"autoDeleteEnabled\":true"));
    }

    @Test
    void updateSuccessUsesDefaultBeforeSnapshotWhenPolicyRowDoesNotExist() {
        MailRetentionPolicyUseCase useCase = useCase();
        MailRetentionPolicy after =
                new MailRetentionPolicy(POLICY_ID, 30, 30, 30, true, ADMIN_ID, NOW);

        given(mailRetentionPolicyRepositoryPort.findById(POLICY_ID)).willReturn(Optional.empty());
        given(mailRetentionPolicyRepositoryPort.save(any())).willReturn(after);

        useCase.update(command(30, true));

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(auditCaptor.capture());
        AdminAuditLog auditLog = auditCaptor.getValue();
        assertTrue(auditLog.beforeValue().contains("\"retentionDays\":365"));
        assertTrue(auditLog.beforeValue().contains("\"autoDeleteEnabled\":false"));
        assertTrue(auditLog.afterValue().contains("\"retentionDays\":30"));
        assertTrue(auditLog.afterValue().contains("\"autoDeleteEnabled\":true"));
    }

    @Test
    void updateFailsWhenRetentionDaysIsZero() {
        MailRetentionPolicyUseCase useCase = useCase();

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.update(command(0, true)));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void updateFailsWhenRetentionDaysExceedsMax() {
        MailRetentionPolicyUseCase useCase = useCase();

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.update(command(3651, true)));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    private MailRetentionPolicyUseCase useCase() {
        return new MailRetentionPolicyUseCase(
                mailRetentionPolicyRepositoryPort,
                adminAuditLogRepositoryPort,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private MailRetentionPolicy policy(int retentionDays, boolean autoDeleteEnabled) {
        return new MailRetentionPolicy(
                POLICY_ID,
                retentionDays,
                retentionDays,
                retentionDays,
                autoDeleteEnabled,
                ADMIN_ID,
                Instant.parse("2026-06-01T00:00:00Z"));
    }

    private MailRetentionPolicyCommand command(int retentionDays, boolean autoDeleteEnabled) {
        return new MailRetentionPolicyCommand(
                retentionDays, autoDeleteEnabled, ADMIN_ID, "Admin", "127.0.0.1", "JUnit");
    }
}
