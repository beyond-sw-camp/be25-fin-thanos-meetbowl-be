package com.meetbowl.domain.admin;

import java.util.Optional;
import java.util.UUID;

public interface AdminAuditLogRepositoryPort {

    AdminAuditLog save(AdminAuditLog adminAuditLog);

    Optional<AdminAuditLog> findById(UUID auditLogId);
}
