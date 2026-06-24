package com.meetbowl.application.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.common.response.ErrorDetail;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;

@Service
public class AdminOrganizationMembersExcelAuditService {

    private final AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    private final ObjectMapper objectMapper;

    public AdminOrganizationMembersExcelAuditService(
            AdminAuditLogRepositoryPort adminAuditLogRepositoryPort, ObjectMapper objectMapper) {
        this.adminAuditLogRepositoryPort = adminAuditLogRepositoryPort;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(
            UUID adminId,
            String adminName,
            String ipAddress,
            String userAgent,
            String fileName,
            List<ErrorDetail> details,
            String message) {
        adminAuditLogRepositoryPort.save(
                new AdminAuditLog(
                        UUID.randomUUID(),
                        adminId,
                        adminName,
                        "ORGANIZATION_MEMBER_EXCEL",
                        null,
                        null,
                        null,
                        "ORGANIZATION_MEMBER_EXCEL",
                        "IMPORT",
                        AuditResult.FAILURE,
                        null,
                        toJson(
                                new FailureSnapshot(
                                        fileName,
                                        message,
                                        details.size(),
                                        details.stream().limit(20).toList())),
                        ipAddress,
                        userAgent,
                        Instant.now()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR, "Failed to serialize admin audit snapshot.");
        }
    }

    private record FailureSnapshot(
            String fileName, String message, int errorCount, List<ErrorDetail> details) {}
}
