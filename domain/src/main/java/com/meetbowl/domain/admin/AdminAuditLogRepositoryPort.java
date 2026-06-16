package com.meetbowl.domain.admin;

import java.util.Optional;
import java.util.UUID;

import com.meetbowl.domain.common.Paged;

public interface AdminAuditLogRepositoryPort {

    AdminAuditLog save(AdminAuditLog adminAuditLog);

    Optional<AdminAuditLog> findById(UUID auditLogId);

    Paged<AdminAuditLog> findPage(AdminAuditLogSearchCondition condition);
}
