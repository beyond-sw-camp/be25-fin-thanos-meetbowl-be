package com.meetbowl.application.mail;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.mail.MailRetentionPolicy;
import com.meetbowl.domain.mail.MailRetentionPolicyRepositoryPort;

@Service
public class MailRetentionPolicyUseCase {

    private static final UUID SYSTEM_POLICY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final int DEFAULT_RETENTION_DAYS = 365;
    private static final int MAX_RETENTION_DAYS = 3650;

    private final MailRetentionPolicyRepositoryPort mailRetentionPolicyRepositoryPort;
    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MailRetentionPolicyUseCase(
            MailRetentionPolicyRepositoryPort mailRetentionPolicyRepositoryPort,
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort,
            ObjectMapper objectMapper,
            Clock clock) {
        this.mailRetentionPolicyRepositoryPort = mailRetentionPolicyRepositoryPort;
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MailRetentionPolicyResult get() {
        return mailRetentionPolicyRepositoryPort
                .findById(SYSTEM_POLICY_ID)
                .map(this::toResult)
                .orElse(new MailRetentionPolicyResult(DEFAULT_RETENTION_DAYS, false, null, null));
    }

    @Transactional
    public MailRetentionPolicyResult update(MailRetentionPolicyCommand command) {
        validateRetentionDays(command.retentionDays());

        MailRetentionPolicyResult before =
                mailRetentionPolicyRepositoryPort
                        .findById(SYSTEM_POLICY_ID)
                        .map(this::toResult)
                        .orElse(
                                new MailRetentionPolicyResult(
                                        DEFAULT_RETENTION_DAYS, false, null, null));

        Instant now = Instant.now(clock);
        MailRetentionPolicy saved =
                mailRetentionPolicyRepositoryPort.save(
                        new MailRetentionPolicy(
                                SYSTEM_POLICY_ID,
                                command.retentionDays(),
                                command.retentionDays(),
                                command.retentionDays(),
                                command.autoDeleteEnabled(),
                                command.adminId(),
                                now));

        MailRetentionPolicyResult after = toResult(saved);
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        command.adminId(),
                        command.adminName(),
                        "MAIL_RETENTION_POLICY",
                        SYSTEM_POLICY_ID,
                        null,
                        null,
                        "MAIL_RETENTION_POLICY",
                        "UPDATE",
                        AuditResult.SUCCESS,
                        snapshot(before),
                        snapshot(after),
                        command.ipAddress(),
                        command.userAgent(),
                        now));
        return after;
    }

    private void validateRetentionDays(int retentionDays) {
        if (retentionDays < 1) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Retention days must be at least 1.");
        }
        if (retentionDays > MAX_RETENTION_DAYS) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    "Retention days must be less than or equal to " + MAX_RETENTION_DAYS + ".");
        }
    }

    private MailRetentionPolicyResult toResult(MailRetentionPolicy policy) {
        return new MailRetentionPolicyResult(
                policy.inboxRetentionDays(),
                policy.autoDeleteEnabled(),
                policy.updatedAt(),
                policy.updatedBy());
    }

    private String snapshot(MailRetentionPolicyResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }
}
