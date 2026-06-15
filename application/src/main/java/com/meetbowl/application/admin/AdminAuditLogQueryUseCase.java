package com.meetbowl.application.admin;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AdminAuditLogSearchCondition;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.domain.common.Paged;

@Service
public class AdminAuditLogQueryUseCase {

    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;

    public AdminAuditLogQueryUseCase(AdminAuditLogRepositoryPort adminAuditLogRepositoryPort) {
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
    }

    @Transactional(readOnly = true)
    public AdminAuditLogPageResult search(AdminAuditLogSearchCommand command) {
        AuditResult result = parseResult(command.result());
        AdminAuditLogSearchCondition condition =
                new AdminAuditLogSearchCondition(
                        command.actorUserId(),
                        blankToNull(command.actorName()),
                        blankToNull(command.actionType()),
                        blankToNull(command.targetType()),
                        command.targetId(),
                        result,
                        command.from(),
                        command.to(),
                        command.page(),
                        command.size());

        Paged<AdminAuditLog> page = adminAuditLogRepositoryPort.findPage(condition);
        return new AdminAuditLogPageResult(
                page.content().stream().map(AdminAuditLogResult::from).toList(),
                command.page(),
                command.size(),
                page.totalElements(),
                calculateTotalPages(page.totalElements(), command.size()));
    }

    @Transactional(readOnly = true)
    public AdminAuditLogResult get(UUID auditLogId) {
        return adminAuditLogRepositoryPort
                .findById(auditLogId)
                .map(AdminAuditLogResult::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
    }

    private AuditResult parseResult(String result) {
        String normalized = blankToNull(result);
        if (normalized == null) {
            return null;
        }
        try {
            if ("FAILED".equals(normalized)) {
                return AuditResult.FAILURE;
            }
            return AuditResult.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Unsupported audit result.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
