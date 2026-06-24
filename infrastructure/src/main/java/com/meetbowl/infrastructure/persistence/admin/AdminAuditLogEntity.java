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

/** 관리자 활동에 대한 감사 로그를 저장하는 엔티티다. 누가(actor)가 어떤 대상(target)에 어떤 작업을 수행했고 결과가 무엇이었는지를 기록한다. */
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLogEntity extends BaseEntity {
    /** 감사 작업을 수행한 관리자 사용자 ID(UUID). */
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID actorId;

    /** 감사 작업을 수행한 관리자 이름(스냅샷). */
    @Column(nullable = false, length = 100)
    private String actorName;

    /** 대상 타입(예: USER, ORGANIZATION 등). 도메인별 식별 문자열. */
    @Column(nullable = false, length = 100)
    private String targetType;

    /** 대상 엔티티의 ID(UUID). 없을 수 있어 null 허용. */
    @Column(columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(length = 100)
    private String targetLoginId;

    @Column(length = 100)
    private String targetName;

    /** 작업 영역(예: AUTH, USER_MANAGEMENT 등). */
    @Column(nullable = false, length = 100)
    private String actionArea;

    /** 작업 이름(예: CREATE, UPDATE, DELETE). */
    @Column(nullable = false, length = 100)
    private String actionName;

    /** 작업 결과(성공/실패 등). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditResult result;

    /** 변경 전 값(JSON 또는 텍스트). */
    @Column(columnDefinition = "TEXT")
    private String beforeValue;

    /** 변경 후 값(JSON 또는 텍스트). */
    @Column(columnDefinition = "TEXT")
    private String afterValue;

    /** 작업 요청자 IP 주소. */
    @Column(length = 100)
    private String ipAddress;

    /** 요청자 User-Agent 헤더. */
    @Column(length = 500)
    private String userAgent;

    /** 작업이 실제로 발생한 시각(UTC). */
    @Column(nullable = false)
    private Instant occurredAt;

    protected AdminAuditLogEntity() {}

    private AdminAuditLogEntity(AdminAuditLog log) {
        actorId = log.actorId();
        actorName = log.actorName();
        targetType = log.targetType();
        targetId = log.targetId();
        targetLoginId = log.targetLoginId();
        targetName = log.targetName();
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
                targetLoginId,
                targetName,
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
