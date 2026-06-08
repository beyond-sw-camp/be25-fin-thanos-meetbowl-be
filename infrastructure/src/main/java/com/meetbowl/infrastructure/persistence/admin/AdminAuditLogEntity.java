package com.meetbowl.infrastructure.persistence.admin;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AuditResult;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLogEntity extends BaseEntity {
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(nullable = false, length = 100)
    private String actorName;

    @Column(nullable = false, length = 100)
    private String targetType;

    @Column(columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(nullable = false, length = 100)
    private String actionArea;

    @Column(nullable = false, length = 100)
    private String actionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditResult result;

    @Column(columnDefinition = "TEXT")
    private String beforeValue;

    @Column(columnDefinition = "TEXT")
    private String afterValue;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private Instant occurredAt;

    protected AdminAuditLogEntity() {}

    private AdminAuditLogEntity(AdminAuditLog log) {
        actorId = log.actorId();
        actorName = log.actorName();
        targetType = log.targetType();
        targetId = log.targetId();
        actionArea = log.actionArea();
        actionName = log.actionName();
        result = log.result();
        beforeValue = log.beforeValue();
        afterValue = log.afterValue();
        ipAddress = log.ipAddress();
        userAgent = log.userAgent();
        occurredAt = log.occurredAt();
    }

    static AdminAuditLogEntity from(AdminAuditLog log) {
        AdminAuditLogEntity entity = new AdminAuditLogEntity(log);
        entity.setId(log.id());
        return entity;
    }

    AdminAuditLog toDomain() {
        return new AdminAuditLog(
                getId(),
                actorId,
                actorName,
                targetType,
                targetId,
                actionArea,
                actionName,
                result,
                beforeValue,
                afterValue,
                ipAddress,
                userAgent,
                occurredAt);
    }
}
